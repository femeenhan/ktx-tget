package dev.trainassist.korail

import android.content.Context
import android.content.SharedPreferences

object AutomationPreferences {
    private const val PREFS_NAME: String = "train_assist_prefs"
    private const val KEY_ARMED: String = "automation_armed"
    fun isAutomationArmed(ctx: Context): Boolean {
        return prefs(ctx).getBoolean(KEY_ARMED, false)
    }
    fun setAutomationArmed(ctx: Context, armed: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ARMED, armed).apply()
    }
    private fun prefs(ctx: Context): SharedPreferences {
        return ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
