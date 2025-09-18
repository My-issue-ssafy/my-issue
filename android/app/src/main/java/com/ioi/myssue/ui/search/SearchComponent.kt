package com.ioi.myssue.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.news.NewsItem
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background400

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val onSearchUpdated by rememberUpdatedState(onSearch)

    fun performSearch() {
        onSearchUpdated()
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = typography.titleMedium,
        prefix = { Spacer(Modifier.width(4.dp)) },
        leadingIcon = null,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "clear",
                        )
                    }
                } else {
                    Spacer(Modifier.size(44.dp))
                }
                IconButton(
                    onClick = { performSearch() },
                    modifier = Modifier
                        .size(44.dp)
                        .padding(end = 10.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "search",
                        modifier = Modifier
                            .size(36.dp)
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = stringResource(R.string.search_news),
                style = typography.titleMedium,
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BackgroundColors.Background250,
            unfocusedContainerColor = BackgroundColors.Background250,
            disabledContainerColor = BackgroundColors.Background250,
            // 밑줄(인디케이터) 제거
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            // 선택 커서 색
            cursorColor = AppColors.Primary600,
            // 텍스트 색
            focusedTextColor = BackgroundColors.Background500,
            unfocusedTextColor = BackgroundColors.Background500,
            disabledTextColor = BackgroundColors.Background500,
            errorTextColor = BackgroundColors.Background500,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    )


}

@Composable
fun CategoryChipsRow(
    selected: NewsCategory,
    onSelect: (NewsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.width(4.dp))

        NewsCategory.entries.forEach { cat ->
            FilterChip(
                selected = cat == selected,
                onClick = { onSelect(cat) },
                label = {
                    Box(
                        modifier = Modifier.widthIn(min = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.category,
                            style = typography.bodyLarge,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                shape = CircleShape,
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = BackgroundColors.Background250,
                    labelColor = AppColors.Primary600,
                    selectedContainerColor = AppColors.Primary400,
                    selectedLabelColor = BackgroundColors.Background50,
                ),
                modifier = Modifier
                    .height(35.dp)
                    .defaultMinSize(minWidth = 70.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
fun SearchedEmpty() {
    Box(
        Modifier
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.no_searched_news),
            style = typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Background400,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}