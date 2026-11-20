package app.steptwelve.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import app.steptwelve.AppContainer
import app.steptwelve.StepTwelveApp

/** Gives ViewModel initializers access to the app's [AppContainer]. */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StepTwelveApp).container
