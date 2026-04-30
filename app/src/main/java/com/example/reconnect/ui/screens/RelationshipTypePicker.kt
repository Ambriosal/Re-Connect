package com.example.reconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reconnect.data.model.RelationshipType

@Composable
fun RelationshipTypePicker(
    selectedKey: String,                        // the currently stored key
    onTypeSelected: (RelationshipType) -> Unit  // fires when user picks one
) {
    // Resolve the current key back to a type object for selected-state comparison
    val selectedType = RelationshipType.fromKey(selectedKey)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)  // cap so it doesn't overflow dialogs
    ) {
        items(RelationshipType.all) { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text("${type.emoji} ${type.label}") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}