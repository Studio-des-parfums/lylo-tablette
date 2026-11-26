package com.lylo.tablet

import android.app.Application
import com.lylo.tablet.robot.RobotController

class LyloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sans effet sur une tablette sans services robot : les actions robot restent masquées.
        RobotController.connect(this)
    }
}
