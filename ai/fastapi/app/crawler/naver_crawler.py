# -*- coding: utf-8 -*-
"""
Naver 뉴스 크롤러 (완성본 / 본문 위치기반 이미지 인라인 + KO-BERT 제목 임베딩 저장)

이 크롤러는 네이버 뉴스 사이트에서 기사를 수집하여 JSONL 형식으로 저장합니다.
- 기사 본문은 blocks/markdown 두 가지 모드 중 선택 가능
- 저장: 발행일 기준 JSONL (output/YYYYMMDD.jsonl)
- 중복 제거: 기사(oid/aid) 기준
- 안정성: Session+Retry, Referer, 무작위 슬립, 페이지 HTML 해시 비교
- [NEW] 기사당 제목을 KoBERT(768차원)로 임베딩해서 JSONL에 함께 저장

필수 패키지(최초 1회):
pip install transformers torch sentencepiece
"""

# 필요한 라이브러리 import
import os, re, json, time, hashlib, random
from datetime import datetime, timedelta, timezone
from urllib.parse import urljoin, urlparse, parse_qs, unquote

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from bs4 import BeautifulSoup
from bs4.element import NavigableString, Tag

# ========================
# 전역 설정 변수들
# ========================

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/127.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
}

MAX_PAGES = 50
CRAWL_BACK_DAYS = 1
LIST_DELAY = (0.4, 0.9)
DETAIL_DELAY = (0.6, 1.3)

INLINE_IMAGES_MODE = "blocks"  # "markdown" 가능

# 네이버 뉴스 메인 섹션 코드 (sid1)
NAVER_SECTIONS = ["100", "101", "102", "103", "104", "105"]

SID1_TO_SECTION = {
    "100": "정치",
    "101": "경제",
    "102": "사회",
    "103": "생활/문화",
    "104": "세계",
    "105": "IT/과학"
}

PC_LIST_WRAPPERS = [
    "div#main_content div.list_body",
    "div#main_content ul.type06_headline",
    "div#main_content ul.type06",
    "div#main_content ul",
]

ARTICLE_ID_RE = re.compile(r"(?:https?://n\.news\.naver\.com)?/article/(\d{3})/(\d{9,10})(?:[?#].*)?$")
READ_QUERY_RE = re.compile(r"(?:https?://news\.naver\.com)?/main/read\.naver")

IMAGE_EXTS = (".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg", ".avif", ".heic")
IMG_URL_RE = re.compile(
    r"https?://[^\s\"'>)]+?(?:"
    r"\.jpg|\.jpeg|\.png|\.gif|\.webp|\.bmp|\.svg|\.avif|\.heic)"
    r"(?:\?[^\s\"'>)]*)?", re.IGNORECASE
)

DOWNLOAD_IMAGES = False
IMAGES_DIR = "images"

# ========================
# [NEW] KO-BERT 제목 임베딩 설정/유틸
# ========================

EMBEDDING_ENABLED = True
EMBED_MODEL_NAME = "monologg/kobert"  # 768-dim

_KOBERT_TOKENIZER = None
_KOBERT_MODEL = None
_KOBERT_DEVICE = None

def _get_kobert():
    """
    전역 1회 로딩: 토크나이저/모델/디바이스
    """
    global _KOBERT_TOKENIZER, _KOBERT_MODEL, _KOBERT_DEVICE
    if _KOBERT_MODEL is None or _KOBERT_TOKENIZER is None:
        from transformers import AutoTokenizer, AutoModel
        import torch
        _KOBERT_DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
        # 👇 여기 trust_remote_code=True 추가
        _KOBERT_TOKENIZER = AutoTokenizer.from_pretrained(
            EMBED_MODEL_NAME,
            trust_remote_code=True
        )
        _KOBERT_MODEL = AutoModel.from_pretrained(
            EMBED_MODEL_NAME,
            trust_remote_code=True
        ).to(_KOBERT_DEVICE)
        _KOBERT_MODEL.eval()
        print(f"[EMB] loaded: {EMBED_MODEL_NAME} on {_KOBERT_DEVICE}")
    return _KOBERT_TOKENIZER, _KOBERT_MODEL, _KOBERT_DEVICE

def embed_title(text: str):
    """
    제목 → 768차원 KoBERT 임베딩(list[float])
    - mean-pooling + L2 정규화(코사인 유사도용)
    """
    if not EMBEDDING_ENABLED or not text:
        return None
    from torch.nn.functional import normalize
    import torch
    tok, mdl, device = _get_kobert()
    inputs = tok(
        text,
        return_tensors="pt",
        truncation=True,
        padding=True,
        max_length=128
    ).to(device)
    with torch.no_grad():
        last_hidden = mdl(**inputs).last_hidden_state   # [1, L, 768]
    mask = inputs["attention_mask"].unsqueeze(-1)       # [1, L, 1]
    vec = (last_hidden * mask).sum(dim=1) / mask.sum(dim=1).clamp(min=1e-9)  # mean-pooling
    vec = normalize(vec, p=2, dim=1)                    # L2 정규화
    return vec.squeeze(0).cpu().tolist()                # 768 floats

# ========================
# HTTP 세션 및 안전한 GET
# ========================

def make_session() -> requests.Session:
    s = requests.Session()
    retries = Retry(
        total=3,
        connect=3,
        read=3,
        backoff_factor=0.7,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=["HEAD","GET","OPTIONS"]
    )
    adapter = HTTPAdapter(max_retries=retries, pool_connections=100, pool_maxsize=100)
    s.mount("http://", adapter)
    s.mount("https://", adapter)
    s.headers.update(HEADERS)
    return s

SESSION = make_session()

def rand_sleep(bounds):
    lo, hi = bounds
    time.sleep(random.uniform(lo, hi))

def safe_get(url: str, timeout: float = 15.0, extra_retries: int = 2, referer: str | None = None):
    last_exc = None
    for i in range(extra_retries + 1):
        try:
            headers = HEADERS.copy()
            if referer:
                headers["Referer"] = referer
            headers["Cache-Control"] = "no-cache"
            headers["Pragma"] = "no-cache"
            return SESSION.get(url, timeout=timeout, headers=headers)
        except requests.RequestException as e:
            last_exc = e
            sleep = (2 ** i) * 0.7 + random.random() * 0.3
            print(f"[NET-RETRY] {url} -> {e} | retry in {sleep:.1f}s")
            time.sleep(sleep)
    raise last_exc

# ========================
# 유틸리티
# ========================

def yyyymmdd_from_iso(iso_str: str | None) -> str | None:
    if not iso_str:
        return None
    s = iso_str.strip()
    if re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}", s):
        return s[:10].replace("-", "")
    if re.fullmatch(r"\d{14}", s):
        return s[:8]
    digits = re.findall(r"\d", s)
    return "".join(digits[:8]) if len(digits) >= 8 else None

def append_jsonl(path: str, item: dict):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "a", encoding="utf-8") as f:
        f.write(json.dumps(item, ensure_ascii=False) + "\n")

def _abs_url(base: str, u: str) -> str:
    if not u:
        return ""
    if u.startswith("//"):
        return "https:" + u
    return urljoin(base, u)

def canonicalize_article_url(u: str) -> tuple[str | None, tuple[str, str] | None]:
    if not u:
        return None, None
    m = ARTICLE_ID_RE.search(u)
    if m:
        oid, aid = m.group(1), m.group(2)
        return f"https://n.news.naver.com/article/{oid}/{aid}", (oid, aid)
    if READ_QUERY_RE.search(u):
        pr = urlparse(u)
        qs = parse_qs(pr.query)
        oid = (qs.get("oid") or [""])[0]
        aid = (qs.get("aid") or [""])[0]
        if re.fullmatch(r"\d{3}", oid) and re.fullmatch(r"\d{9,10}", aid):
            return f"https://n.news.naver.com/article/{oid}/{aid}", (oid, aid)
    return None, None

# ========================
# 본문 파싱 보조
# ========================

COPYRIGHT_RE = re.compile(r"(무단전재|재배포 금지|저작권)")

def _normalize_img_url(u: str, base_url: str) -> str | None:
    if not u:
        return None
    u = _abs_url(base_url, u.strip())
    core = u.split("?", 1)[0].lower()
    if not core.endswith(IMAGE_EXTS):
        if "/image/" in core or "/img/" in core:
            return u
        return None
    return u

def _text_clean(s: str) -> str:
    # \n은 살리고, 나머지 연속 공백만 축소
    s = re.sub(r"[ \t]+", " ", s)   # 스페이스/탭만 정리
    s = re.sub(r"\n{3,}", "\n\n", s)  # 줄바꿈이 3개 이상 → 2개
    s = s.strip()
    if not s:
        return ""
    if COPYRIGHT_RE.search(s):
        return ""
    return s


def _body_root_candidates(soup: BeautifulSoup):
    cands = [
        "#newsct_article",
        "article#dic_area",
        "div#articeBody",
        "div#articleBody",
        "div#newsEndContents",
    ]
    for sel in cands:
        node = soup.select_one(sel)
        if node:
            return node
    return soup

# --- blocks 모드
def build_body_blocks(soup: BeautifulSoup, base_url: str) -> list[dict]:
    root = _body_root_candidates(soup)
    blocks = []
    buf = []

    def flush_buf():
        nonlocal buf
        txt = _text_clean("".join(buf))
        if txt:
            blocks.append({"type": "text", "content": txt})
        buf = []

    def walk(node: Tag | NavigableString):
        if isinstance(node, NavigableString):
            buf.append(str(node))
            return
        if not isinstance(node, Tag):
            return
        name = (node.name or "").lower()
        if name in ("style", "script", "noscript", "iframe"):
            return
        if name in ("br",):
            buf.append("\n")
        if name in ("img",):
            u = node.get("data-src") or node.get("src") or node.get("data-origin")
            u = _normalize_img_url(u, base_url)
            if u:
                flush_buf()
                blocks.append({"type": "image", "content": u})
            return
        if name == "em" and "img_desc" in (node.get("class") or []):
            desc = node.get_text(" ", strip=True)
            if desc:
                flush_buf()
                blocks.append({"type": "img_desc", "content": desc})
            return
        
        if name in ("picture", "figure"):
            for child in node.children:
                walk(child)
            return
        cls = " ".join(node.get("class") or [])
        if any(key in cls for key in ["_VOD_PLAYER_WRAP", "as_addinfo", "media_end_linked", "promotion"]):
            return
        for child in node.children:
            walk(child)
        if name in ("p", "div", "li", "section", "article", "blockquote"):
            buf.append("\n")

    for child in root.children:
        walk(child)
    flush_buf()

    cleaned = []
    for b in blocks:
        if b["type"] == "text":
            t = b["content"].strip()
            if not t:
                continue
            cleaned.append({"type": "text", "content": t})
        else:
            cleaned.append(b)
    return cleaned

def build_body_markdown(soup: BeautifulSoup, base_url: str) -> str | None:
    blocks = build_body_blocks(soup, base_url)
    parts = []
    for b in blocks:
        if b["type"] == "text":
            parts.append(b["content"])
        else:
            parts.append(f"![]({b['content']})")
    md = "\n\n".join(parts).strip()
    return md or None

# --- 메타 파싱
def parse_title(soup: BeautifulSoup) -> str | None:
    h = soup.select_one("h2.media_end_head_headline, #newsct_article h2")
    if h:
        t = h.get_text(" ", strip=True)
        if t:
            return t
    ogt = soup.find("meta", {"property": "og:title"})
    return ogt["content"].strip() if ogt and ogt.get("content") else None

def parse_regdate_iso(soup: BeautifulSoup) -> str | None:
    for tag in soup.find_all("script", {"type": "application/ld+json"}):
        try:
            data = json.loads(tag.string or tag.text or "")
            items = data if isinstance(data, list) else [data]
            for it in items:
                if not isinstance(it, dict):
                    continue
                t = it.get("@type") or it.get("@type".lower())
                if (t == "NewsArticle") or ("Article" in str(t)):
                    dp = (it.get("datePublished") or it.get("dateCreated") or it.get("uploadDate"))
                    if dp:
                        dp = str(dp).strip().replace(" ", "T")
                        return dp.split("+")[0].split("Z")[0]
        except Exception:
            continue
    for key in [
        {"property": "article:published_time"},
        {"name": "date"},
        {"name": "publication_date"},
        {"itemprop": "datePublished"},
    ]:
        m = soup.find("meta", key)
        if m and m.get("content"):
            iso = m["content"].strip().replace(" ", "T")
            return iso.split("+")[0].split("Z")[0]
    t = soup.select_one("span.media_end_head_info_datestamp_time, time")
    if t:
        for attr in ["datetime", "data-date-time", "data-date", "data-datetime"]:
            v = t.get(attr)
            if v:
                v = v.strip().replace(" ", "T")
                return v.split("+")[0].split("Z")[0]
        txt = t.get_text(" ", strip=True)
        digits = re.findall(r"\d", txt)
        if len(digits) >= 12:
            y = "".join(digits[:4]); M = "".join(digits[4:6]); d = "".join(digits[6:8])
            h = "".join(digits[8:10]); mi = "".join(digits[10:12])
            return f"{y}-{M}-{d}T{h}:{mi}:00"
    return None

def parse_press(soup: BeautifulSoup) -> str | None:
    for tag in soup.find_all("script", {"type": "application/ld+json"}):
        try:
            data = json.loads(tag.string or tag.text or "")
            items = data if isinstance(data, list) else [data]
            for it in items:
                if not isinstance(it, dict):
                    continue
                pub = it.get("publisher")
                if isinstance(pub, dict):
                    name = pub.get("name")
                    if name:
                        return str(name).strip()
        except Exception:
            continue
    logo_img = soup.select_one("a.media_end_head_top_logo img")
    if logo_img and logo_img.get("alt"):
        return logo_img["alt"].strip()
    auth = soup.find("meta", {"property": "og:article:author"}) or soup.find("meta", {"name": "author"})
    if auth and auth.get("content"):
        return auth["content"].strip()
    return None

# 흔한 성씨 집합
_SURNAMES_1 = set(list("김이박최정강조윤장임오한신서권황안송전홍유고문양손배백허남심노하곽성차주우구민류진엄채원천방변함염여육석탁두빈동온위표기반용마왕"))
_SURNAMES_2 = {"남궁", "제갈", "선우", "황보", "사공", "서문", "독고"}

def _looks_like_korean_person_name(name: str) -> bool:
    if not name:
        return False
    n = re.sub(r"\s+", "", str(name))
    noise_words = {"대통령실", "기자단", "사진기자", "영상", "제공", "연결", "보도국", "데스크"}
    if n in noise_words:
        return False
    if not re.fullmatch(r"[가-힣]+", n):
        return False
    for fx in _SURNAMES_2:
        if n.startswith(fx) and re.fullmatch(r"[가-힣]{3,4}", n):
            return True
    if n[0] not in _SURNAMES_1:
        return False
    return bool(re.fullmatch(r"[가-힣]{2,4}", n))

def parse_reporter(soup: BeautifulSoup) -> str | None:
    """
    네이버 뉴스에서 기자 이름을 안정적으로 추출
    - 1차: em.media_end_head_journalist_name
    - 2차: span.byline_s
    - 3차: 기자카드 em.media_journalistcard_summary_name_text
    - 4차: JSON-LD 메타데이터
    - 5차: 다양한 CSS selector
    - 6차: 전체 텍스트 정규식 fallback
    """

    # --------------------------
    # 1) 기본 헤드라인 기자
    el = soup.select_one("em.media_end_head_journalist_name")
    if el:
        txt = el.get_text(strip=True).replace(" 기자", "").strip()
        if txt and _looks_like_korean_person_name(txt):
            return txt

    # --------------------------
    # 2) 바이라인(span.byline_s)
    byline = soup.select_one("span.byline_s")
    if byline:
        txt = byline.get_text(strip=True)
        if txt:
            txt = txt.split("(")[0].replace(" 기자", "").strip()
            if txt and _looks_like_korean_person_name(txt):
                return txt

    # --------------------------
    # 3) 기자카드(em.media_journalistcard_summary_name_text)
    card = soup.select_one("em.media_journalistcard_summary_name_text")
    if card:
        txt = card.get_text(strip=True).replace(" 기자", "").strip()
        if txt and _looks_like_korean_person_name(txt):
            return txt

    # --------------------------
    # 4) JSON-LD 메타데이터(author)
    for tag in soup.find_all("script", {"type": "application/ld+json"}):
        try:
            data = json.loads(tag.string or tag.text or "")
            items = data if isinstance(data, list) else [data]
            for it in items:
                if isinstance(it, dict) and it.get("author"):
                    authors = it["author"] if isinstance(it["author"], list) else [it["author"]]
                    for a in authors:
                        if isinstance(a, dict):
                            name = str(a.get("name") or "").replace(" 기자", "").strip()
                        else:
                            name = str(a).replace(" 기자", "").strip()
                        if name and _looks_like_korean_person_name(name):
                            return name
        except Exception:
            continue

    # --------------------------
    # 5) 추가 selector 후보들
    cands = [
        ".journalist_name", ".reporter_name", ".author", ".byline",
        ".journalistcard_summary_name", "div.byline span",
        "div.byline", "p.byline_p", 'meta[name="author"]',
    ]
    for sel in cands:
        el = soup.select_one(sel)
        if not el:
            continue
        txt = (el.get("content") if getattr(el, "name", "") == "meta" else el.get_text(" ", strip=True)) or ""
        txt = re.sub(r"\S+@\S+", " ", txt)
        txt = re.sub(r"\s+", " ", txt).strip()
        if txt and "기자" in txt:
            m = re.search(r"([가-힣]{2,4})", txt)
            if m and _looks_like_korean_person_name(m.group(1)):
                return m.group(1)

    # --------------------------
    # 6) 최후 fallback: 전체 텍스트 정규식
    full = soup.get_text(" ", strip=True)
    m = re.search(r"([가-힣]{2,4})\s*기자", full)
    if m and _looks_like_korean_person_name(m.group(1)):
        return m.group(1)

    return None

# ========================
# 기사 상세 페이지 수집 및 파싱
# ========================

def fetch_and_parse(url: str, sid1: str | None = None) -> dict | None:
    canon_url, id_tuple = canonicalize_article_url(url)
    target = canon_url or url
    r = safe_get(target, timeout=15.0, referer="https://news.naver.com/")
    if r.status_code != 200:
        return None
    soup = BeautifulSoup(r.text, "lxml")

    item: dict = {
        "url": canon_url or target,
        "title": parse_title(soup),
        "published_at": parse_regdate_iso(soup),
        "press": parse_press(soup),
        "reporter": parse_reporter(soup),
        "section": [SID1_TO_SECTION.get(sid1, "기타")] if sid1 else [],
        "crawled_at": datetime.now(timezone.utc).isoformat(),
    }

    if INLINE_IMAGES_MODE == "blocks":
        item["body"] = build_body_blocks(soup, base_url=item["url"])
    else:
        item["body"] = build_body_markdown(soup, base_url=item["url"])

    if DOWNLOAD_IMAGES and isinstance(item.get("body"), list):
        oid, aid = (id_tuple or ("", ""))
        article_key = f"{oid}_{aid}" if oid and aid else hashlib.md5(item["url"].encode("utf-8")).hexdigest()[:10]
        pub_yyyymmdd = yyyymmdd_from_iso(item.get("published_at")) or "unknown"
        idx = 0
        for b in item["body"]:
            if b["type"] == "image":
                idx += 1
                iu = b["content"]
                ext = os.path.splitext(iu.split("?", 1)[0])[1] or ".jpg"
                local_path = os.path.join(IMAGES_DIR, pub_yyyymmdd, f"{article_key}_{idx:02d}{ext}")
                try:
                    os.makedirs(os.path.dirname(local_path), exist_ok=True)
                    rr = safe_get(iu, timeout=20, referer=item["url"])
                    if rr.status_code == 200 and rr.content:
                        with open(local_path, "wb") as f:
                            f.write(rr.content)
                except Exception as e:
                    print("[IMG-ERR]", iu, e)

    return item

# ========================
# 기사 링크 수집
# ========================

def extract_links_from_pc_html(html: str) -> list[str]:
    soup = BeautifulSoup(html, "lxml")
    hrefs: set[str] = set()
    wrappers = []
    for sel in PC_LIST_WRAPPERS:
        wrappers.extend(soup.select(sel))
    for w in wrappers:
        for a in w.find_all("a", href=True):
            hrefs.add(a["href"].strip())
    if not hrefs:
        wrapper = soup.select_one("#main_content")
        if wrapper:
            for a in wrapper.find_all("a", href=True):
                hrefs.add(a["href"].strip())
    if not hrefs:
        for a in soup.find_all("a", href=True):
            hrefs.add(a["href"].strip())
    canon: set[str] = set()
    for h in hrefs:
        cu, _id = canonicalize_article_url(h)
        if cu and _id:
            canon.add(cu)
    return list(canon)

def discover_links(sid1: str, reg_date: str, max_pages: int = MAX_PAGES) -> list[str]:
    found: set[str] = set()
    same_html_streak = 0
    prev_hash = None
    for page in range(1, max_pages + 1):
        list_url = f"https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1={sid1}&date={reg_date}&page={page}"
        try:
            ref = f"https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1={sid1}&date={reg_date}&page={page-1}" if page > 1 \
                else f"https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1={sid1}&date={reg_date}"
            r = safe_get(list_url, timeout=12.0, referer=ref)
        except Exception as e:
            print(f"[ERR] discover sid1={sid1}/{reg_date} p{page} -> {e}")
            break
        if r.status_code != 200:
            print(f"[WARN] {list_url} -> {r.status_code}")
            break
        html = r.text
        h = hashlib.sha1(html.encode("utf-8", "ignore")).hexdigest()
        new_links = extract_links_from_pc_html(html)
        before = len(found)
        for u in new_links:
            found.add(u)
        page_new = len(found) - before
        print(f"[sid1:{sid1}][{reg_date}][p{page}] so_far={len(found)} page_new={page_new} hash={h[:8]}")
        if prev_hash == h or page_new == 0:
            same_html_streak += 1
        else:
            same_html_streak = 0
        prev_hash = h
        if same_html_streak >= 2:
            break
        rand_sleep(LIST_DELAY)
    return list(found)

# ========================
# 저장 헬퍼(oid/aid 기준)
# ========================
def save_item_if_new(item: dict, seen_ids: set[str]) -> bool:
    cu, id_tuple = canonicalize_article_url(item.get("url") or "")
    if not id_tuple:
        return False
    oid, aid = id_tuple
    key = f"{oid}/{aid}"
    if key in seen_ids:
        return False
    pub_yyyymmdd = yyyymmdd_from_iso(item.get("published_at"))
    out_path = os.path.join("output", f"{pub_yyyymmdd}.jsonl") if pub_yyyymmdd else os.path.join("output", "unknown.jsonl")
    append_jsonl(out_path, item)
    seen_ids.add(key)
    return True

# ========================
# 메인
# ========================
if __name__ == "__main__":
    """
    네이버 뉴스 크롤러 (리스트 페이지 수집 전용)
    """
    today = datetime.today()
    reg_dates = [(today - timedelta(days=i)).strftime("%Y%m%d") for i in range(CRAWL_BACK_DAYS)]
    seen_ids: set[str] = set()

    print(f"▶ 리스트 페이지 전용 수집 시작: days={CRAWL_BACK_DAYS}, max_pages={MAX_PAGES}, sections={len(NAVER_SECTIONS)}")

    for reg in reg_dates:
        print(f"\n=== 날짜: {reg} (list-only) ===")
        for sid1 in NAVER_SECTIONS:
            try:
                urls = discover_links(sid1, reg, max_pages=MAX_PAGES)
            except KeyboardInterrupt:
                raise
            except Exception as e:
                print(f"[ERR] discover sid1={sid1}/{reg} -> {e}")
                continue

            for u in urls:
                try:
                    item = fetch_and_parse(u, sid1=sid1)
                    if not item:
                        continue

                    # --- [NEW] 제목 임베딩 추가 (KO-BERT 768차원) ---
                    try:
                        emb = embed_title(item.get("title") or "")
                        if emb:
                            item["title_embedding"] = {
                                "model": EMBED_MODEL_NAME,
                                "vector": emb,     # 768 floats
                                "dim": len(emb),   # 768
                                "normalized": True
                            }
                    except Exception as e:
                        print("[EMB-ERR] title embedding:", e)
                    # --- [NEW end] ---

                    if save_item_if_new(item, seen_ids):
                        n_imgs = 0
                        if isinstance(item.get("body"), list):
                            n_imgs = sum(1 for b in item["body"] if b.get("type") == "image")
                        elif isinstance(item.get("body"), str):
                            n_imgs = len(IMG_URL_RE.findall(item["body"]))
                        print(f"[LIST][{yyyymmdd_from_iso(item.get('published_at')) or 'unknown'}] {item.get('title')} (sid1={sid1}) | imgs={n_imgs} | sections={item.get('section')}")
                        rand_sleep(DETAIL_DELAY)
                except KeyboardInterrupt:
                    raise
                except Exception as e:
                    print("[ERR]", u, e)

    print("✔ 리스트 전용 크롤링 완료!")
    print(f"  - 고유 기사 수: {len(seen_ids):,}개 (중복 제거됨)")
    print("  - 저장 위치: output/ (발행일별 JSONL 파일)")
    print(f"  - 본문 모드: {INLINE_IMAGES_MODE} ({'블록 배열' if INLINE_IMAGES_MODE == 'blocks' else '마크다운 문자열'})")
    if DOWNLOAD_IMAGES:
        print(f"  - 이미지 저장 위치: {IMAGES_DIR}/ (날짜별 디렉토리)")