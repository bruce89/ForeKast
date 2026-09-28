package dev.bruze.forekast

import android.app.Application
import dev.bruze.forekast.di.AppContainer

class ForeKastApplication : Application() {
    val container by lazy { AppContainer(this) }
}
