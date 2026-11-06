package com.cerejaplay.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.cerejaplay.app.data.model.AppConfig
import com.cerejaplay.app.data.repo.DefaultConfig

val LocalAppConfig = staticCompositionLocalOf<AppConfig> { DefaultConfig.value }
