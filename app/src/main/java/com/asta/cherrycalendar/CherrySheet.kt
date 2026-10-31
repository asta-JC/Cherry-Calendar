package com.asta.cherrycalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/** Native dragging on the handle, independent scrolling in the body. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CherrySheet(
    onDismiss: () -> Unit,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Stop unused body scroll/fling velocity before it reaches Material's sheet connection.
    // In particular, a fast fling at the top must not drag the whole sheet down then settle up.
    val bodyBoundary = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = Offset(0f, available.y)
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = Velocity(0f, available.y)
        }
    }
    val currentDismiss by rememberUpdatedState(onDismiss)
    ModalBottomSheet(
        onDismissRequest = { if (dismissEnabled) currentDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = dismissEnabled,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        // No clickable wrapper, ripple, or long-press tooltip. The parent owns native dragging.
        Box(
            Modifier.fillMaxWidth().height(44.dp).semantics {
                contentDescription = "Sheet drag handle"
                if (dismissEnabled) dismiss { currentDismiss(); true }
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(32.dp, 4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)))
        }
        Column(Modifier.fillMaxWidth().nestedScroll(bodyBoundary), content = content)
    }
}
