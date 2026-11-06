package com.justinlin099.pdx206statusbarhookyaap

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** Standalone Launcher module; no status-bar or Android framework resource hooks. */
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == LauncherRecentsHook.PACKAGE_NAME &&
            lpparam.processName == LauncherRecentsHook.PACKAGE_NAME) {
            LauncherRecentsHook.install(lpparam)
        }
    }
}
