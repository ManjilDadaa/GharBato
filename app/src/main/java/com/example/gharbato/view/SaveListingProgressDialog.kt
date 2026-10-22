package com.example.gharbato.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gharbato.ui.theme.GharBatoButtonDefaults
import com.example.gharbato.ui.theme.LocalGharBatoActionColors

@Composable
internal fun SaveListingProgressDialog(
    isSaving: Boolean,
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onKeepEditing: () -> Unit,
    saveError: String? = null
) {
    val actions = LocalGharBatoActionColors.current
    Dialog(
        onDismissRequest = { if (!isSaving) onKeepEditing() },
        properties = DialogProperties(
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = !isSaving,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .semantics { paneTitle = "Save your progress?" },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Save your progress?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Save a draft and finish your listing later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                saveError?.takeIf { it.isNotBlank() }?.let { message ->
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { if (!isSaving) onSaveAndExit() },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics {
                        if (isSaving) {
                            stateDescription = "Saving draft"
                            liveRegion = LiveRegionMode.Polite
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actions.container,
                        contentColor = actions.onContainer,
                        disabledContainerColor = actions.container,
                        disabledContentColor = actions.onContainer
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp).clearAndSetSemantics { },
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isSaving) "Saving…" else "Save and exit", textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { if (!isSaving) onKeepEditing() },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = GharBatoButtonDefaults.outlinedColors(),
                    border = GharBatoButtonDefaults.outlinedBorder(enabled = !isSaving)
                ) {
                    Text("Keep editing", textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { if (!isSaving) onExitWithoutSaving() },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Exit without saving", textAlign = TextAlign.Center)
                }
            }
        }
    }
}
