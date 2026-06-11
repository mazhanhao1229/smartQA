package com.example.smartqa

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt 依赖注入入口 —— 必须加到 AndroidManifest.xml 的 <application> 中。 */
@HiltAndroidApp
class SmartQaApplication : Application()
