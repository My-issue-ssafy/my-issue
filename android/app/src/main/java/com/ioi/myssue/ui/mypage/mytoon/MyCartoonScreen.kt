package com.ioi.myssue.ui.mypage.mytoon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioi.myssue.R
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.ui.cartoon.CartoonCard
import com.ioi.myssue.ui.cartoon.ExpandedCartoonCard

@Composable
fun MyCartoonScreen(
    viewModel: MyCartoonViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TwoColumnGridWithLazyVerticalGrid(
            items = state.myToons,
            onClick = viewModel::setClickedToon
        )

        state.clickedToon?.let {
            BackHandler {
                state.clickedToon?.let {
                    viewModel.setClickedToon(null)
                }
            }

            ExpandedCartoonCard(
                cartoon = it,
                isExiting = false,
                isFlippable = false,
                exitDir = 0,
                modifier = Modifier,
                onClick = {}
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "item.newsTitle")
                    Image(
                        painter = painterResource(R.drawable.ic_heart),
                        contentDescription = null
                    )
                }

            }
        }
    }

}

@Composable
fun TwoColumnGridWithLazyVerticalGrid(
    items: List<CartoonNews>,
    onClick: (CartoonNews) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2열 고정
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            CartoonCard(
                cartoon = item,
                isExiting = false,
                exitDir = 0,
                modifier = Modifier,
                isFlippable = false,
                onClick = { onClick(item) }
            )
        }
    }
}
