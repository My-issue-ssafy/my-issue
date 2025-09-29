import os
import uuid
from pathlib import Path
from typing import Optional
from loguru import logger


def ensure_directory_exists(directory: str) -> None:
    """Ensure directory exists, create if it doesn't"""
    Path(directory).mkdir(parents=True, exist_ok=True)


def generate_unique_filename(extension: str = "wav") -> str:
    """Generate unique filename with given extension"""
    return f"{uuid.uuid4().hex}.{extension}"


def get_temp_audio_path(filename: Optional[str] = None) -> str:
    """Get path for temporary audio file"""
    temp_dir = "temp_audio"
    ensure_directory_exists(temp_dir)

    if filename is None:
        filename = generate_unique_filename("wav")

    return os.path.join(temp_dir, filename)


def cleanup_old_files(directory: str, max_age_hours: int = 24) -> None:
    """Remove files older than specified hours"""
    import time
    current_time = time.time()
    max_age_seconds = max_age_hours * 3600

    if not os.path.exists(directory):
        return

    for filename in os.listdir(directory):
        file_path = os.path.join(directory, filename)
        if os.path.isfile(file_path):
            file_age = current_time - os.path.getctime(file_path)
            if file_age > max_age_seconds:
                try:
                    os.remove(file_path)
                    logger.info(f"Removed old file: {file_path}")
                except OSError:
                    pass


def get_file_size(file_path: str) -> int:
    """Get file size in bytes"""
    try:
        return os.path.getsize(file_path)
    except OSError:
        return 0