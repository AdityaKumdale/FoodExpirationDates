package com.lorenzovainigli.foodexpirationdates.view

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lorenzovainigli.foodexpirationdates.BuildConfig
import com.lorenzovainigli.foodexpirationdates.R
import com.lorenzovainigli.foodexpirationdates.di.AppModule
import com.lorenzovainigli.foodexpirationdates.di.DaggerAppComponent
import com.lorenzovainigli.foodexpirationdates.model.NotificationManager
import com.lorenzovainigli.foodexpirationdates.model.PreferencesProvider
import com.lorenzovainigli.foodexpirationdates.model.entity.ExpirationDate
import com.lorenzovainigli.foodexpirationdates.model.worker.CheckExpirationsWorker
import com.lorenzovainigli.foodexpirationdates.ui.theme.FoodExpirationDatesTheme
import com.lorenzovainigli.foodexpirationdates.view.composable.DropdownMenu
import com.lorenzovainigli.foodexpirationdates.view.composable.FoodCard
import com.lorenzovainigli.foodexpirationdates.view.composable.MyTopAppBar
import com.lorenzovainigli.foodexpirationdates.viewmodel.ExpirationDateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.min

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerAppComponent.builder()
            .appModule(AppModule())
            .build()
        NotificationManager.setupNotificationChannel(this)
    }

    override fun onResume() {
        super.onResume()
        setContent {
            val viewModel: ExpirationDateViewModel = viewModel()
            val dates by viewModel.getDates().collectAsState(emptyList())
            MainActivityLayout(dates)
        }
    }

    @Composable
    fun MainActivityLayout(
        items: List<ExpirationDate>? = null,
        viewModel: ExpirationDateViewModel? = viewModel(),
        deleteExpirationDate: ((ExpirationDate) -> Unit)? = viewModel!!::deleteExpirationDate,
    ) {
        val context = LocalContext.current
        if (viewModel != null) {
            scheduleDailyCheckOfExpirations(viewModel.getDates())
        }
        FoodExpirationDatesTheme {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Scaffold(
                    topBar = {
                        MyTopAppBar(
                            title = stringResource(id = R.string.app_name),
                            actions = {
                                DropdownMenu()
                            }
                        )
                    },
                    floatingActionButtonPosition = FabPosition.End,
                    floatingActionButton = {
                        if (items != null && items.isNotEmpty()) {
                            FloatingActionButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            InsertActivity::class.java
                                        )
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                ) { padding ->
                    if (items != null && items.isNotEmpty()) {
                        Column(Modifier.padding(padding)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                for (item in items) {
                                    FoodCard(
                                        item = item,
                                        onClickEdit = {
                                            val intent = Intent(context, InsertActivity::class.java)
                                            intent.putExtra("ITEM_ID", item.id)
                                            context.startActivity(intent)
                                        },
                                        onClickDelete = {
                                            if (deleteExpirationDate != null) {
                                                deleteExpirationDate(item)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(
                                space = 16.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_items_found),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.Gray.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(id = R.string.please_insert_one),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                modifier = Modifier.size(50.dp),
                                contentPadding = PaddingValues(0.dp),
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            InsertActivity::class.java
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleDailyCheckOfExpirations(dates: Flow<List<ExpirationDate>>) {
        lifecycleScope.launch {
            val sb = StringBuilder()
            dates.collect { list ->
                val today = Calendar.getInstance()
                val twoDaysAgo = Calendar.getInstance()
                twoDaysAgo.add(Calendar.DAY_OF_MONTH, -2)
                val yesterday = Calendar.getInstance()
                yesterday.add(Calendar.DAY_OF_MONTH, -1)
                val tomorrow = Calendar.getInstance()
                tomorrow.add(Calendar.DAY_OF_MONTH, 1)
                val msInADay = (1000 * 60 * 60 * 24)
                val filteredList = list.filter {
                    it.expirationDate < tomorrow.time.time
                }
                if (filteredList.isEmpty()){
                    return@collect
                }
                filteredList.map {
                    sb.append(it.foodName).append(" (")
                    if (it.expirationDate < twoDaysAgo.time.time) {
                        val days = (today.time.time - it.expirationDate) / msInADay
                        sb.append(getString(R.string.n_days_ago, days))
                    } else if (it.expirationDate < yesterday.time.time)
                        sb.append(getString(R.string.yesterday).lowercase())
                    else if (it.expirationDate < today.time.time){
                        sb.append(getString(R.string.today).lowercase())
                    } else {
                        sb.append(getString(R.string.tomorrow).lowercase())
                    }
                    sb.append("), ")
                }
                val currentTime = Calendar.getInstance()
                val dueTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, PreferencesProvider.getUserNotificationTimeHour(
                        applicationContext))
                    set(Calendar.MINUTE, PreferencesProvider.getUserNotificationTimeMinute(
                        applicationContext))
                    set(Calendar.SECOND, 0)
                }
                if (currentTime > dueTime)
                    dueTime.add(Calendar.DAY_OF_MONTH, 1)
                var message = ""
                if (sb.toString().length > 2)
                    message = sb.toString().substring(0, sb.toString().length - 2) + "."
                val inputData = workDataOf("message" to message)
                val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis
                val formattedTime = formatTimeDifference(initialDelay)
                if (BuildConfig.DEBUG) {
                    Toast.makeText(
                        applicationContext,
                        "Notification in $formattedTime",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val workRequest = PeriodicWorkRequestBuilder<CheckExpirationsWorker>(
                    1, TimeUnit.DAYS
                )
                    .setInputData(inputData)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(applicationContext)
                    .enqueueUniquePeriodicWork(
                        CheckExpirationsWorker.workerID,
                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                        workRequest
                    )
            }
        }
    }

    private fun formatTimeDifference(timeDifference: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(timeDifference)
        val hours = TimeUnit.MILLISECONDS.toHours(timeDifference) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference) % 60
        val formattedTime = StringBuilder()
        if (days > 0) {
            formattedTime.append("$days day${if (days > 1) "s" else ""} ")
        }
        if (hours > 0) {
            formattedTime.append("$hours hour${if (hours > 1) "s" else ""} ")
        }
        if (minutes > 0) {
            formattedTime.append("$minutes minute${if (minutes > 1) "s" else ""} ")
        }
        if (seconds > 0) {
            formattedTime.append("$seconds second${if (seconds > 1) "s" else ""} ")
        }
        return formattedTime.toString().trim()
    }


    private fun getItemsForPreview(lang: String = "en"): List<ExpirationDate> {
        val items = ArrayList<ExpirationDate>()
        var foods = arrayOf("Eggs", "Cheese", "Milk", "Ham", "Butter", "Mushrooms", "Tomato")
        if (lang == "it")
            foods = arrayOf("Uova", "Formaggio", "Latte", "Prosciutto", "Funghi", "Pomodori")
        val daysLeft = arrayOf(-1, 0, 1, 3, 7, 10, 30)
        for (i in 0 until min(foods.size, daysLeft.size)) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, daysLeft[i])
            items.add(
                ExpirationDate(
                    id = 0,
                    foodName = foods[i],
                    expirationDate = cal.time.time
                )
            )
        }
        return items
    }

    @Preview(showBackground = true, wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
    @Composable
    fun DefaultPreview() {
        val items = getItemsForPreview()
        MainActivityLayout(
            items = items,
            viewModel = null,
            deleteExpirationDate = null
        )
    }

    @Preview(
        name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true,
        wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
    )
    @Composable
    fun PreviewDarkMode() {
        DefaultPreview()
    }

    @Preview(name = "Italian", locale = "it", showBackground = true)
    @Composable
    fun PreviewItalian() {
        DefaultPreview()
    }

    @Preview(name = "Arabic", locale = "ar", showBackground = true)
    @Composable
    fun PreviewArabic() {
        DefaultPreview()
    }

    @Preview(name = "German", locale = "de", showBackground = true)
    @Composable
    fun PreviewGerman() {
        DefaultPreview()
    }

    @Preview(name = "Hindi", locale = "hi", showBackground = true)
    @Composable
    fun PreviewHindi() {
        DefaultPreview()
    }

    @Preview(name = "Spanish", locale = "es", showBackground = true)
    @Composable
    fun PreviewSpanish() {
        DefaultPreview()
    }

}