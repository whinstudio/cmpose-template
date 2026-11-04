package com.kturker.multiplatform

import androidx.compose.runtime.Composable
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.hint
import com.kturker.multiplatform.core.multilang.resource.localServer
import com.kturker.multiplatform.core.network.LOCAL_SERVER_RUN_COMMAND
import com.kturker.multiplatform.core.network.localServerAdbReverseCommand

// LocalServerBanner's text in the app's language; the commands are never translated
@Composable
internal fun localServerHint(): String =
    LocalStringResources.current.localServer.hint(LOCAL_SERVER_RUN_COMMAND, localServerAdbReverseCommand)
