package com.test.presentation.util

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Int.hSpace() = Spacer(modifier = Modifier.height(this.dp))

@Composable
fun Int.wSpace() = Spacer(modifier = Modifier.width(this.dp))

@Composable
fun Int.space() = Spacer(modifier = Modifier.size(this.dp))
