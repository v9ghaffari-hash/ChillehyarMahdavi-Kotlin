package ir.chillehyar.mahdi

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Forest = Color(0xFF173A31)
private val Forest2 = Color(0xFF285647)
private val Gold = Color(0xFFC39A52)
private val Cream = Color(0xFFF7F4EC)
private val CardCream = Color(0xFFFFFDF8)
private val Ink = Color(0xFF25362F)
private val Muted = Color(0xFF7A827B)
private val PaleGold = Color(0xFFF0E5CF)
private val Rose = Color(0xFFB46B63)

private val defaultHabits = listOf("قرائت قرآن", "خوش‌اخلاقی", "دعا برای فرج")
private val dailyTasks = listOf("نمازهایم را با توجه خواندم", "قرآن و دعا خواندم", "برای امام زمان(عج) دعا کردم", "از گناه و عادت نامطلوب دوری کردم", "به دیگران نیکی کردم", "محاسبهٔ نفس انجام دادم")
private val habitTasks = listOf(
    listOf("نماز صبح", "نماز ظهر", "نماز عصر", "نماز مغرب", "نماز عشا", "نماز اول وقت", "حضور قلب", "تعقیبات", "نماز مستحبی", "محاسبهٔ نماز"),
    listOf("قرآن صبحگاهی", "ترجمهٔ آیات", "تدبر در آیات", "حفظ یک آیه", "تلاوت روزانه", "سورهٔ عهد", "آیت‌الکرسی", "هدیهٔ ثواب", "عمل به یک آیه", "مرور آموخته‌ها"),
    listOf("صلوات روزانه", "دعای فرج", "دعای عهد", "ذکر استغفار", "ذکر یا مهدی", "صدقه برای سلامتی", "یاد امام زمان", "زیارت آل‌یاسین", "دعا برای مؤمنان", "تجدید عهد"),
    listOf("کنترل زبان", "کنترل نگاه", "ترک غیبت", "ترک دروغ", "کنترل خشم", "ترک اسراف", "کنترل فضای مجازی", "دوری از بدگمانی", "حفظ حق‌الناس", "توبه و جبران"),
    listOf("کمک به خانواده", "احوال‌پرسی", "دستگیری نیازمند", "لبخند و خوش‌رفتاری", "انتقال خیر", "رسیدگی به والدین", "رفع کدورت", "کار خیر پنهانی", "رعایت حق دیگران", "نیت خدمت")
)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("chillehyar_native_v30", MODE_PRIVATE) }
    private var revision by mutableIntStateOf(0)
    private var page by mutableStateOf("خانه")
    private var selectedDay by mutableIntStateOf(1)
    private var selectedHabit by mutableIntStateOf(0)
    private var noteDraft by mutableStateOf("")
    private var slipDraft by mutableStateOf("")
    private var customHabitDraft by mutableStateOf("")
    private var customTargetDraft by mutableStateOf("")
    private var customSpecialDraft by mutableStateOf("")
    private var customEmergencyDraft by mutableStateOf("")
    private var reminderHour by mutableIntStateOf(8)
    private var reminderMinute by mutableIntStateOf(0)
    private var reminderEnabled by mutableStateOf(false)
    private var selectedVirtueDate by mutableStateOf("۱۴۰۵/۰۸/۱۰")
    private var motivationTitleDraft by mutableStateOf("کارهای انگیزشی امروز")
    private var motivationDateDraft by mutableStateOf("۱۴۰۵/۰۸/۱۰")
    private var tacticNameDraft by mutableStateOf("")
    private var tacticSolutionDraft by mutableStateOf("")
    private var tacticCauseDraft by mutableStateOf(setOf<Int>())
    private var themeChoice by mutableIntStateOf(0)
    private var uiScale by mutableFloatStateOf(1f)
    private val virtueHeaders = listOf("واجب", "حرام", "مستحب", "مکروه", "اخلاق", "سلامتی", "شغلی", "تفریحی", "خانواده", "علمی", "متفرقه")
    private val motivationItems = listOf("یادآوری اهداف", "تفکر", "کتاب", "کلاس", "کلاس اخلاق و موعظه", "سخنرانی", "آیات قرآن", "احادیث", "دعا", "توسل", "ارتباط با امام زمان (عج)", "اشک بر امام حسین (ع)", "تلقین مثبت", "ارتباط با افراد موفق", "همیار", "کمک به دیگران")
    private val tacticCauses = listOf("گام به گام نبودن برنامه", "تسویف و به تعویق انداختن", "فراموشی هدف و نیت", "تصمیم شتاب‌زده", "دوری از همراهان خوب", "ترس از شروع", "ناامیدی پس از شکست", "مانع‌تراشی در عبادت", "زیبا جلوه دادن کار نادرست")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderEnabled = prefs.getBoolean("reminder_enabled", false)
        themeChoice = prefs.getInt("theme_choice", 0)
        uiScale = prefs.getFloat("ui_scale", 1f)
        reminderHour = prefs.getInt("reminder_hour", 8)
        reminderMinute = prefs.getInt("reminder_minute", 0)
        createNotificationChannel()
        // Rebuild alarms when the app is opened as a safety net after updates or device-specific alarm cleanup.
        if (reminderEnabled) ReminderReceiver.schedule(this, prefs)
        habitNames().forEachIndexed { index, name ->
            if (prefs.getBoolean("habit_reminder_${index}_on", false)) {
                ReminderReceiver.scheduleHabit(
                    this, index, name,
                    prefs.getString("habit_reminder_${index}_time", "20:00") ?: "20:00",
                    prefs.getString("habit_reminder_${index}_text", "") ?: ""
                )
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 29)
        }
        setContent { ChillehyarApp() }
    }

    private fun keyDay(day: Int, task: Int) = "day_${day}_task_$task"
    private fun isTaskDone(day: Int, task: Int) = prefs.getBoolean(keyDay(day, task), false)
    private fun setTask(day: Int, task: Int, value: Boolean) { prefs.edit().putBoolean(keyDay(day, task), value).apply(); revision++ }
    private fun dayDoneCount(day: Int) = dailyTasks.indices.count { isTaskDone(day, it) }
    private fun completedDays() = (1..40).count { dayDoneCount(it) == dailyTasks.size }
    private fun currentDay(): Int {
        val start = prefs.getLong("start_date", 0L)
        if (start == 0L) return 1
        return (((System.currentTimeMillis() - start) / 86_400_000L).toInt() + 1).coerceIn(1, 40)
    }
    private fun startChallenge() { if (prefs.getLong("start_date", 0L) == 0L) prefs.edit().putLong("start_date", System.currentTimeMillis()).apply(); revision++ }
    private fun habitNames(): List<String> {
        val extra = prefs.getString("custom_habits", "")!!.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return (defaultHabits + extra).mapIndexed { index, name -> prefs.getString("habit_name_override_$index", name) ?: name }
    }
    private fun taskKey(habit: Int, task: Int, day: Int) = "habit_${habit}_task_${task}_day_${day}"
    private fun taskDone(habit: Int, task: Int, day: Int) = prefs.getBoolean(taskKey(habit, task, day), false)
    private fun setHabitTask(habit: Int, task: Int, day: Int, value: Boolean) { prefs.edit().putBoolean(taskKey(habit, task, day), value).apply(); revision++ }
    private fun noteKey(day: Int) = "note_day_$day"
    private fun notes(): List<Pair<Int, String>> = (1..40).mapNotNull { d -> prefs.getString(noteKey(d), null)?.takeIf { it.isNotBlank() }?.let { d to it } }.reversed()
    private fun saveNote(day: Int, text: String) { prefs.edit().putString(noteKey(day), text).apply(); revision++ }
    private fun saveSlip(day: Int, text: String) { prefs.edit().putString("slip_day_$day", text).apply(); revision++ }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(ReminderReceiver.CHANNEL, "یادآورهای چله‌یار", NotificationManager.IMPORTANCE_HIGH).apply { description = "یادآور روزانه و عهد چله‌یار مهدوی"; enableVibration(true) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun configureReminder(enabled: Boolean, hour: Int, minute: Int) {
        reminderEnabled = enabled; reminderHour = hour; reminderMinute = minute
        prefs.edit().putBoolean("reminder_enabled", enabled).putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
        val alarm = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(this, ReminderReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (enabled) {
            val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
            if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pending) else alarm.set(AlarmManager.RTC_WAKEUP, c.timeInMillis, pending)
        } else alarm.cancel(pending)
        revision++
    }
    private fun deleteCustomHabit(index: Int) {
        if (index < defaultHabits.size) return
        val extras = (prefs.getString("custom_habits", "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
        val extraIndex = index - defaultHabits.size
        if (extraIndex !in extras.indices) return
        val oldCount = defaultHabits.size + extras.size
        extras.removeAt(extraIndex)
        val editor = prefs.edit()
        val prefixes = listOf("h400_", "h400_row_", "hmeta_", "habit_note_", "habit_reminder_", "habit_name_override_", "habit_" )
        val all = prefs.all
        // Remove the selected habit's index-based keys, then shift later habit data down one slot.
        all.keys.forEach { key ->
            prefixes.forEach { prefix ->
                if (key.startsWith(prefix)) {
                    val tail = key.removePrefix(prefix)
                    val digits = tail.takeWhile { it.isDigit() }
                    if (digits.isNotEmpty()) {
                        val parsed = digits.toIntOrNull()
                        if (parsed == index) editor.remove(key)
                    }
                }
            }
        }
        for (oldIndex in index + 1 until oldCount) {
            all.forEach { (key, value) ->
                prefixes.forEach { prefix ->
                    if (key.startsWith(prefix)) {
                        val tail = key.removePrefix(prefix)
                        val digits = tail.takeWhile { it.isDigit() }
                        if (digits.toIntOrNull() == oldIndex) {
                            val newKey = prefix + (oldIndex - 1) + tail.drop(digits.length)
                            when (value) { is Boolean -> editor.putBoolean(newKey,value); is Int -> editor.putInt(newKey,value); is Long -> editor.putLong(newKey,value); is Float -> editor.putFloat(newKey,value); is String -> editor.putString(newKey,value) }
                            editor.remove(key)
                        }
                    }
                }
            }
        }
        editor.putString("custom_habits", extras.joinToString("\n")).apply()
        selectedHabit = (defaultHabits.size).coerceAtMost((defaultHabits.size + extras.size - 1).coerceAtLeast(0))
        page = "عادت‌ها"; revision++
    }

    private fun configureHabitReminder(index: Int, habitName: String, enabled: Boolean, time: String, message: String) {
        val parts = time.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0,23) ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0,59) ?: 0
        val alarm = getSystemService(AlarmManager::class.java)
        val requestCode = 10000 + index
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = "ir.chillehyar.mahdi.HABIT_REMINDER"
            putExtra("habit_index", index); putExtra("habit_name", habitName); putExtra("habit_time", "%02d:%02d".format(hour,minute)); putExtra("habit_message", message)
        }
        val pending = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (!enabled) { alarm.cancel(pending); pending.cancel(); return }
        val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,hour); set(Calendar.MINUTE,minute); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0); if(timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1) }
        if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pending) else alarm.set(AlarmManager.RTC_WAKEUP,c.timeInMillis,pending)
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) exportBackup(uri) }
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) importBackup(uri) }
    private fun exportBackup(uri: Uri) {
        try {
            val all = JSONObject()
            val data = JSONObject()
            prefs.all.forEach { (k, v) -> when (v) { is Boolean -> data.put(k, v); is Int -> data.put(k, v); is Long -> data.put(k, v); is Float -> data.put(k, v); is String -> data.put(k, v) } }
            all.put("app", "ChillehyarMahdavi").put("schema", 1).put("exportedAt", System.currentTimeMillis()).put("preferences", data)
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(all.toString(2)) }
            Toast.makeText(this, "پشتیبان با موفقیت ذخیره شد", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "خطا در پشتیبان‌گیری: ${e.message}", Toast.LENGTH_LONG).show() }
    }
    private fun importBackup(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() } ?: error("فایل خوانده نشد")
            val root = JSONObject(text); val data = root.getJSONObject("preferences"); val editor = prefs.edit()
            data.keys().forEach { key -> val value = data.get(key); when (value) { is Boolean -> editor.putBoolean(key, value); is Int -> editor.putInt(key, value); is Long -> editor.putLong(key, value); is String -> editor.putString(key, value); is Double -> editor.putLong(key, value.toLong()) } }
            editor.apply(); reminderEnabled = prefs.getBoolean("reminder_enabled", false); reminderHour = prefs.getInt("reminder_hour", 8); reminderMinute = prefs.getInt("reminder_minute", 0)
            if (reminderEnabled) configureReminder(true, reminderHour, reminderMinute)
            revision++; Toast.makeText(this, "بازیابی اطلاعات انجام شد", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "فایل پشتیبان معتبر نیست: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    @Composable private fun ChillehyarApp() {
        // Read revision to trigger redraws without recreating the whole page/scroll state.
        val observedRevision = revision
        val palette = listOf(Color(0xFF173A31), Color(0xFF176B55), Color(0xFF2F6FBA), Color(0xFF7B61A8), Color(0xFFB96865), Color(0xFF946C2E))
        val chosenPrimary = palette[themeChoice.coerceIn(0, palette.lastIndex)]
        val density = LocalDensity.current
        MaterialTheme(colorScheme = lightColorScheme(primary = chosenPrimary, secondary = Gold, background = Cream, surface = CardCream, onPrimary = Color.White, onBackground = Ink, onSurface = Ink)) {
            CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl, LocalDensity provides androidx.compose.ui.unit.Density(density.density, uiScale.coerceIn(.9f,1.2f))) {
                Scaffold(containerColor = Cream, bottomBar = { BottomNavigationBar(page) { page = it } }) { padding ->
                    if (observedRevision < 0) Spacer(Modifier.height(0.dp))
                    key(page) {
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            HeaderBar(page)
                            when (page) {
                                "خانه" -> HomePage()
                                "چله من" -> ChillaPage()
                                "عادت‌ها" -> HabitsPage()
                                "ثبت روزانه" -> DailyPage()
                                "جزئیات عادت" -> HabitDetailPage()
                                "یادداشت‌ها" -> NotesPage()
                                "گزارش" -> ReportsPage()
                                "یادآورها" -> RemindersPage()
                                "ابزارهای پیشرفته" -> ToolsPage()
                                "جدول فضیلت‌ها" -> VirtuePage()
                                "افزایش انگیزه" -> MotivationPage()
                                "تحلیل لغزش" -> TacticsPage()
                                else -> SettingsPage()
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun HeaderBar(title: String) {
        Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Forest, Forest2))).padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("چله‌یار مهدوی", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(if (title == "خانه") "چهل روز برای عهدی روشن‌تر" else title, color = Color(0xFFE7D8B8), fontSize = 12.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text("م", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        }
    }

    @Composable private fun BottomNavigationBar(selected: String, onSelect: (String) -> Unit) {
        val items = listOf("خانه" to Icons.Filled.Home, "چله من" to Icons.Filled.CalendarMonth, "عادت‌ها" to Icons.Filled.CheckCircle, "ثبت روزانه" to Icons.Filled.EditNote, "بیشتر" to Icons.Filled.Menu)
        NavigationBar(containerColor = CardCream, contentColor = Forest, tonalElevation = 8.dp) {
            items.forEach { (label, icon) ->
                NavigationBarItem(selected = if (label == "بیشتر") selected in listOf("یادداشت‌ها", "گزارش", "یادآورها", "تنظیمات", "ابزارهای پیشرفته", "جدول فضیلت‌ها", "افزایش انگیزه", "تحلیل لغزش") else selected == label, onClick = { if (label == "بیشتر") page = "ابزارهای پیشرفته" else onSelect(label) }, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = PaleGold, unselectedIconColor = Muted))
            }
        }
    }

    @Composable private fun ScrollPage(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), content = content) }
    @Composable private fun PremiumCard(content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CardCream), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) { Column(Modifier.padding(16.dp), content = content) } }
    @Composable private fun SectionTitle(title: String, subtitle: String? = null) { Column(Modifier.padding(bottom = 10.dp)) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Forest); if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Muted) } }
    @Composable private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = PaleGold)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Forest, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text(label, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center) } } }
    @Composable private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) { Button(onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest)) { Text(text, fontWeight = FontWeight.SemiBold) } }

    @Composable private fun HomePage() {
        val day = currentDay(); val done = dayDoneCount(day); val percent = done / dailyTasks.size.toFloat()
        ScrollPage {
            Card(Modifier.fillMaxWidth().padding(bottom = 14.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Forest)) {
                Column(Modifier.background(Brush.linearGradient(listOf(Forest, Forest2))).padding(20.dp)) {
                    Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp)); Text("عهد امروز من با امام زمان (عج)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp)); Text("هر روز، یک قدم کوچک؛ هر قدم، نزدیک‌تر به خوبی.", color = Color(0xFFE5E8E0), fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("روز ${day.coerceIn(1,40)} از ۴۰", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("${(completedDays() * 100 / 40)}٪ روزهای کامل", color = Color(0xFFE5D2A8), fontSize = 12.sp) }
                        Box(Modifier.size(74.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text("${(percent * 100).toInt()}٪", color = Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                    }
                    Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { completedDays() / 40f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Gold, trackColor = Color.White.copy(alpha = .18f))
                    Spacer(Modifier.height(16.dp)); PrimaryButton("ثبت اعمال امروز", { selectedDay = day; page = "ثبت روزانه" }, Modifier.fillMaxWidth())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("${completedDays()}", "روز کامل", Modifier.weight(1f)); StatTile("${habitNames().size}", "عادت فعال", Modifier.weight(1f)); StatTile("${notes().size}", "یادداشت", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            PremiumCard { SectionTitle("چک‌لیست امروز", "با ثبت هر مورد، پیشرفت روزانه‌ات را ببین")
                dailyTasks.forEachIndexed { index, task -> Row(Modifier.fillMaxWidth().clickable { setTask(day, index, !isTaskDone(day,index)) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isTaskDone(day,index), onCheckedChange = { setTask(day,index,it) }, colors = CheckboxDefaults.colors(checkedColor = Forest)); Text(task, Modifier.weight(1f), fontSize = 14.sp, color = if (isTaskDone(day,index)) Muted else Ink) } }
            }
            PremiumCard { SectionTitle("ادامهٔ مسیر", "میان‌بُر به بخش‌های مهم")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("عادت‌ها", Icons.Filled.CheckCircle, { page = "عادت‌ها" }, Modifier.weight(1f)); QuickAction("یادداشت", Icons.Filled.EditNote, { page = "یادداشت‌ها" }, Modifier.weight(1f)); QuickAction("گزارش", Icons.Filled.Insights, { page = "گزارش" }, Modifier.weight(1f)) }
            }
        }
    }
    @Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, modifier: Modifier = Modifier) { OutlinedButton(onClick = action, modifier = modifier, shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, PaleGold), contentPadding = PaddingValues(6.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Gold); Text(label, fontSize = 11.sp, color = Forest) } } }

    @Composable private fun ChillaPage() {
        ScrollPage {
            PremiumCard { SectionTitle("چلهٔ چهل‌روزهٔ من", "تاریخ شروع و پیشرفت شخصی")
                val start = prefs.getLong("start_date", 0L)
                Text(if (start == 0L) "هنوز چله را شروع نکرده‌ای." else "شروع: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(start))}", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp)); PrimaryButton(if (start == 0L) "شروع چلهٔ من" else "چله آغاز شده است", { startChallenge() }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatTile("${currentDay()}", "روز جاری", Modifier.weight(1f)); StatTile("${completedDays()}", "روز تکمیل‌شده", Modifier.weight(1f)); StatTile("${40-completedDays()}", "روز باقی‌ماندهٔ کامل", Modifier.weight(1f)) }
            }
            PremiumCard { SectionTitle("تقویم پیشرفت", "روی هر روز بزن تا ثبت روزانهٔ همان روز باز شود")
                (1..40).chunked(5).forEach { rowDays -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) { rowDays.forEach { day -> val complete = dayDoneCount(day) == dailyTasks.size; val today = day == currentDay(); Box(Modifier.weight(1f).padding(vertical = 4.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(if (complete) Forest else if (today) PaleGold else Cream).border(if (today) 1.dp else 0.dp, Gold, RoundedCornerShape(12.dp)).clickable { selectedDay = day; page = "ثبت روزانه" }, contentAlignment = Alignment.Center) { Text(if (complete) "✓" else day.toString(), color = if (complete) Color.White else Forest, fontWeight = if (today || complete) FontWeight.Bold else FontWeight.Normal) } } } }
            }
        }
    }

    @Composable private fun HabitsPage() {
        val names = habitNames()
        ScrollPage {
            PremiumCard { SectionTitle("عادت‌های نیک", "هر عادت ۱۰ ریزکار دارد؛ پیشرفت برای هر روز ذخیره می‌شود")
                names.forEachIndexed { index, name ->
                    val total = (0 until 400).count { prefs.getString("h400_${index}_$it", "") != "" }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Cream).clickable { selectedHabit = index; page = "جزئیات عادت" }.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(CircleShape).background(PaleGold), contentAlignment = Alignment.Center) { Text(prefs.getString("hmeta_${index}_emoji", "🌱") ?: "🌱", fontSize = 22.sp) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, color = Forest); Text("$total از ۴۰۰ خانه ثبت‌شده", fontSize = 11.sp, color = Muted) }; Icon(Icons.Filled.ChevronLeft, null, tint = Gold) }
                        Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { total / 400f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Gold, trackColor = PaleGold)
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }
            PremiumCard { SectionTitle("ساخت عادت تازه", "نام، سه سطح هدف و سپس ۴۰۰ خانهٔ پیگیری")
                OutlinedTextField(customHabitDraft, { customHabitDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام عادت") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(7.dp)); OutlinedTextField(customTargetDraft, { customTargetDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("لقمه کوچک") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(7.dp)); OutlinedTextField(customSpecialDraft, { customSpecialDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("لقمه ویژه") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(7.dp)); OutlinedTextField(customEmergencyDraft, { customEmergencyDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("لقمه اضطراری") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(8.dp)); PrimaryButton("افزودن عادت", { val name = customHabitDraft.trim(); if (name.isNotEmpty() && name !in names) { val newIndex = names.size; prefs.edit().putString("custom_habits", (prefs.getString("custom_habits", "")!! + "\n" + name).trim()).putString("hmeta_${newIndex}_target",customTargetDraft).putString("hmeta_${newIndex}_special",customSpecialDraft).putString("hmeta_${newIndex}_emergency",customEmergencyDraft).apply(); customHabitDraft = ""; customTargetDraft=""; customSpecialDraft=""; customEmergencyDraft=""; revision++ } }, Modifier.fillMaxWidth())
            }
        }
    }

    @Composable private fun HabitDetailPage() {
        val names = habitNames(); if (names.isEmpty()) return
        val habit = selectedHabit.coerceIn(0, names.lastIndex)
        val completed = (0 until 400).count { prefs.getString("h400_${habit}_$it", "") != "" }
        val small = (0 until 400).count { prefs.getString("h400_${habit}_$it", "") == "s" }
        val special = (0 until 400).count { prefs.getString("h400_${habit}_$it", "") == "v" }
        val emergency = (0 until 400).count { prefs.getString("h400_${habit}_$it", "") == "e" }
        var target by remember(habit) { mutableStateOf(prefs.getString("hmeta_${habit}_target", "") ?: "") }
        var specialTarget by remember(habit) { mutableStateOf(prefs.getString("hmeta_${habit}_special", "") ?: "") }
        var emergencyTarget by remember(habit) { mutableStateOf(prefs.getString("hmeta_${habit}_emergency", "") ?: "") }
        var note by remember(habit) { mutableStateOf(prefs.getString("habit_note_$habit", "") ?: "") }
        var editableName by remember(habit) { mutableStateOf(names[habit]) }
        var editableStartDate by remember(habit) { mutableStateOf(prefs.getString("hmeta_${habit}_date", "") ?: "") }
        var editableEmoji by remember(habit) { mutableStateOf(prefs.getString("hmeta_${habit}_emoji", "🌱") ?: "🌱") }
        var editableColor by remember(habit) { mutableStateOf(prefs.getString("hmeta_${habit}_color", "#176B55") ?: "#176B55") }
        var showDeleteConfirm by remember(habit) { mutableStateOf(false) }
        var reminderOn by remember(habit) { mutableStateOf(prefs.getBoolean("habit_reminder_${habit}_on", false)) }
        var reminderTime by remember(habit) { mutableStateOf(prefs.getString("habit_reminder_${habit}_time", "20:00") ?: "20:00") }
        var reminderText by remember(habit) { mutableStateOf(prefs.getString("habit_reminder_${habit}_text", ReminderReceiver.MESSAGES[habit % ReminderReceiver.MESSAGES.size]) ?: ReminderReceiver.MESSAGES[habit % ReminderReceiver.MESSAGES.size]) }
        ScrollPage {
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(PaleGold), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Spa, null, tint = Forest, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { SectionTitle(names[habit], "جدول ۴۰۰ قدم · ۱۰ ردیف × ۴۰ خانه"); Text("تغییر وضعیت هر خانه با لمس آن", fontSize = 11.sp, color = Muted) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { StatTile("${completed}", "ثبت‌شده", Modifier.weight(1f)); StatTile("${small}", "لقمه کوچک", Modifier.weight(1f)); StatTile("${special}", "ویژه", Modifier.weight(1f)); StatTile("${emergency}", "اضطراری", Modifier.weight(1f)) }
                Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { completed / 400f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Gold, trackColor = PaleGold)
                Text("${completed * 100 / 400}% تکمیل · ${400-completed} خانه باقی‌مانده", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            }
            PremiumCard {
                SectionTitle("مشخصات عادت")
                OutlinedTextField(editableName, { editableName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام عادت") }, singleLine = true)
                Spacer(Modifier.height(7.dp)); PrimaryButton("ذخیرهٔ نام عادت", { if(editableName.isNotBlank()) { prefs.edit().putString("habit_name_override_$habit",editableName.trim()).apply(); val extras = prefs.getString("custom_habits", "")!!.split("\n").filter { it.isNotBlank() }.toMutableList(); if(habit >= defaultHabits.size && habit-defaultHabits.size < extras.size) { extras[habit-defaultHabits.size] = editableName.trim(); prefs.edit().putString("custom_habits",extras.joinToString("\n")).apply() }; revision++; Toast.makeText(this@MainActivity,"نام عادت ذخیره شد",Toast.LENGTH_SHORT).show() } }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp)); OutlinedTextField(editableStartDate, { editableStartDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("تاریخ شروع (شمسی)") }, singleLine = true)
                Spacer(Modifier.height(7.dp)); OutlinedTextField(editableEmoji, { editableEmoji = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نماد عادت؛ مثلاً 🌱 یا 📖") }, singleLine = true)
                Spacer(Modifier.height(7.dp)); OutlinedTextField(editableColor, { editableColor = it }, modifier = Modifier.fillMaxWidth(), label = { Text("رنگ عادت به صورت کد رنگ، مثلاً #176B55") }, singleLine = true)
                Spacer(Modifier.height(7.dp)); OutlinedButton(onClick = { prefs.edit().putString("hmeta_${habit}_date",editableStartDate).putString("hmeta_${habit}_emoji",editableEmoji).putString("hmeta_${habit}_color",editableColor).apply(); revision++; Toast.makeText(this@MainActivity,"مشخصات عادت ذخیره شد",Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("ذخیرهٔ تاریخ، نماد و رنگ") }
                if (habit >= defaultHabits.size) { Spacer(Modifier.height(6.dp)); OutlinedButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose)) { Icon(Icons.Filled.Delete,null); Spacer(Modifier.width(6.dp)); Text("حذف این عادت شخصی") } }
                if (showDeleteConfirm) AlertDialog(onDismissRequest = { showDeleteConfirm = false }, title = { Text("حذف عادت؟") }, text = { Text("عادت و داده‌های وابسته به آن از این دستگاه حذف می‌شود. اگر لازم است، ابتدا نسخهٔ پشتیبان بگیر.") }, confirmButton = { TextButton(onClick = { showDeleteConfirm = false; deleteCustomHabit(habit) }) { Text("حذف", color = Rose) } }, dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") } })
            }
            PremiumCard {
                SectionTitle("سه سطح انجام کار", "برای روزهای عادی، سخت و خیلی سخت")
                OutlinedTextField(target, { target = it }, modifier = Modifier.fillMaxWidth(), label = { Text("لقمه کوچک؛ مقدار معمول") }, shape = RoundedCornerShape(13.dp))
                Spacer(Modifier.height(7.dp)); OutlinedTextField(specialTarget, { specialTarget = it }, modifier = Modifier.fillMaxWidth(), label = { Text("لقمه ویژه؛ مقدار کمتر") }, shape = RoundedCornerShape(13.dp))
                Spacer(Modifier.height(7.dp)); OutlinedTextField(emergencyTarget, { emergencyTarget = it }, modifier = Modifier.fillMaxWidth(), label = { Text("لقمه اضطراری؛ کوچک‌ترین قدم") }, shape = RoundedCornerShape(13.dp))
                Spacer(Modifier.height(8.dp)); PrimaryButton("ذخیرهٔ هدف‌ها", { prefs.edit().putString("hmeta_${habit}_target",target).putString("hmeta_${habit}_special",specialTarget).putString("hmeta_${habit}_emergency",emergencyTarget).apply(); revision++; Toast.makeText(this@MainActivity,"هدف‌های عادت ذخیره شد",Toast.LENGTH_SHORT).show() }, Modifier.fillMaxWidth())
            }
            PremiumCard {
                SectionTitle("جدول پیگیری ۴۰۰ قدم", "۱۰ لقمهٔ کوچک؛ برای هر لقمه، ۴۰ خانهٔ روزانه")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    LegendPill("خالی", Color(0xFFE8ECE8)); LegendPill("✓ کوچک", Color(0xFF3D8B6E)); LegendPill("★ ویژه", Color(0xFFD6A64D)); LegendPill("! اضطراری", Color(0xFFB96865))
                }
                Spacer(Modifier.height(10.dp))
                (0 until 10).forEach { row ->
                    var rowTarget by remember(habit,row) { mutableStateOf(prefs.getString("h400_row_${habit}_$row", "") ?: "") }
                    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(17.dp)).background(Cream).border(1.dp, PaleGold, RoundedCornerShape(17.dp)).padding(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(Forest), contentAlignment = Alignment.Center) { Text("${row+1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(8.dp))
                            Text("لقمهٔ کوچک ${row+1}", fontSize = 13.sp, color = Forest, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(rowTarget, { rowTarget = it; prefs.edit().putString("h400_row_${habit}_$row",it).apply() }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("مثلاً: ۵ دقیقه قرآن یا ۳ آیه", fontSize = 11.sp) }, shape = RoundedCornerShape(10.dp))
                        Spacer(Modifier.height(9.dp))
                        (0 until 40).chunked(5).forEach { week ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                week.forEach { col ->
                                    val idx = row*40+col
                                    val value = prefs.getString("h400_${habit}_$idx", "") ?: ""
                                    val bg = when(value) { "s" -> Color(0xFF3D8B6E); "v" -> Color(0xFFD6A64D); "e" -> Color(0xFFB96865); else -> Color.White }
                                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(9.dp)).background(bg).border(1.dp, if(value.isEmpty()) Color(0xFFD6DED7) else bg, RoundedCornerShape(9.dp)).clickable {
                                        val next = when(value) { "" -> "s"; "s" -> "v"; "v" -> "e"; else -> "" }
                                        prefs.edit().putString("h400_${habit}_$idx",next).apply(); revision++
                                    }, contentAlignment = Alignment.Center) {
                                        Text(when(value) { "s" -> "✓"; "v" -> "★"; "e" -> "!"; else -> (idx+1).toString() }, color = if(value.isEmpty()) Muted else Color.White, fontSize = if(value.isEmpty()) 10.sp else 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                        }
                    }
                }
                Text("برای هر خانه لمس کن: خالی ← لقمه کوچک ✓ ← لقمه ویژه ★ ← لقمه اضطراری ! ← خالی", fontSize = 11.sp, color = Muted, lineHeight = 17.sp)
            }
            PremiumCard {
                SectionTitle("یادآور اختصاصی این عادت")
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("یادآوری برای ${names[habit]}", fontWeight = FontWeight.Bold); Text("تنظیم مستقل برای این عادت", fontSize = 11.sp, color = Muted) }; Switch(reminderOn, { reminderOn = it }) }
                OutlinedTextField(reminderTime, { reminderTime = it }, modifier = Modifier.fillMaxWidth(), label = { Text("ساعت (مثلاً 20:00)") }, singleLine = true)
                Spacer(Modifier.height(7.dp)); OutlinedTextField(reminderText, { reminderText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("پیام یادآور") }, singleLine = true)
                Spacer(Modifier.height(8.dp)); PrimaryButton("ذخیره تنظیمات یادآور", { prefs.edit().putBoolean("habit_reminder_${habit}_on",reminderOn).putString("habit_reminder_${habit}_time",reminderTime).putString("habit_reminder_${habit}_text",reminderText).apply(); configureHabitReminder(habit,names[habit],reminderOn,reminderTime,reminderText.ifBlank { ReminderReceiver.MESSAGES[habit % ReminderReceiver.MESSAGES.size] }); revision++; Toast.makeText(this@MainActivity,if(reminderOn) "یادآور اختصاصی این عادت زمان‌بندی شد" else "یادآور اختصاصی غیرفعال شد",Toast.LENGTH_LONG).show() }, Modifier.fillMaxWidth())
            }
            PremiumCard {
                SectionTitle("یادداشت و بازتاب مسیر")
                OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("چه چیزی کمک کرد؟ چه چیزی سخت بود؟") }, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(8.dp)); PrimaryButton("ذخیره یادداشت عادت", { prefs.edit().putString("habit_note_$habit",note).apply(); revision++; Toast.makeText(this@MainActivity,"یادداشت ذخیره شد",Toast.LENGTH_SHORT).show() }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { (0 until 400).forEach { prefs.edit().remove("h400_${habit}_$it").apply() }; revision++ }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Filled.Refresh,null); Spacer(Modifier.width(6.dp)); Text("شروع دوبارهٔ جدول این عادت") }
            }
        }
    }

    @Composable private fun LegendPill(label: String, color: Color) {
        Row(Modifier.clip(RoundedCornerShape(9.dp)).background(color.copy(alpha=.22f)).padding(horizontal=9.dp, vertical=6.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(5.dp)); Text(label, fontSize = 10.sp, color = Forest) }
    }

    @Composable private fun DailyPage() {
        val day = selectedDay.coerceIn(1,40)
        ScrollPage {
            PremiumCard { SectionTitle("ثبت روزانه", "روز موردنظر را انتخاب کن و اعمالت را ثبت کن")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { selectedDay = (selectedDay - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.ChevronRight, null) }; Text("روز $day از ۴۰", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp); OutlinedButton(onClick = { selectedDay = (selectedDay + 1).coerceAtMost(40) }) { Icon(Icons.Filled.ChevronLeft, null) } }
                Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { dayDoneCount(day) / dailyTasks.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Gold, trackColor = PaleGold)
                Text("${dayDoneCount(day)} از ${dailyTasks.size} مورد انجام شده", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                dailyTasks.forEachIndexed { index, task -> Row(Modifier.fillMaxWidth().clickable { setTask(day,index,!isTaskDone(day,index)) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(isTaskDone(day,index), { setTask(day,index,it) }, colors = CheckboxDefaults.colors(checkedColor = Forest)); Text(task, Modifier.weight(1f), fontSize = 14.sp) } }
            }
            PremiumCard { SectionTitle("محاسبهٔ نفس", "اگر لغزشی رخ داد، با مهربانی و صداقت ثبت کن")
                var slip by remember(day) { mutableStateOf(prefs.getString("slip_day_$day", "") ?: "") }
                OutlinedTextField(slip, { slip = it }, modifier = Modifier.fillMaxWidth(), label = { Text("چه چیزی را می‌خواهم اصلاح کنم؟") }, minLines = 3, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(8.dp)); PrimaryButton("ذخیرهٔ محاسبه", { saveSlip(day, slip); Toast.makeText(this@MainActivity, "محاسبه ذخیره شد", Toast.LENGTH_SHORT).show() }, Modifier.fillMaxWidth())
            }
            PrimaryButton("یادداشت این روز", { page = "یادداشت‌ها" }, Modifier.fillMaxWidth())
        }
    }

    @Composable private fun NotesPage() {
        val day = selectedDay.coerceIn(1,40)
        var note by remember(day) { mutableStateOf(prefs.getString(noteKey(day), "") ?: "") }
        ScrollPage {
            PremiumCard { SectionTitle("یادداشت‌های روزانه", "فکرها، دعاها، تجربه‌ها و عهدهای شخصی")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = { selectedDay = (selectedDay - 1).coerceAtLeast(1) }) { Text("روز قبل") }; Text("روز $day", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold); OutlinedButton(onClick = { selectedDay = (selectedDay + 1).coerceAtMost(40) }) { Text("روز بعد") } }
                Spacer(Modifier.height(10.dp)); OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), label = { Text("یادداشت امروز") }, placeholder = { Text("امروز چه آموختم؟ برای فردا چه عهدی دارم؟") }, shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(10.dp)); PrimaryButton("ذخیرهٔ یادداشت", { saveNote(day,note); Toast.makeText(this@MainActivity, "یادداشت ذخیره شد", Toast.LENGTH_SHORT).show() }, Modifier.fillMaxWidth())
            }
            PremiumCard { SectionTitle("یادداشت‌های ذخیره‌شده")
                val allNotes = notes()
                if (allNotes.isEmpty()) Text("هنوز یادداشتی ثبت نشده است.", color = Muted)
                allNotes.forEach { (d, text) -> Column(Modifier.fillMaxWidth().padding(vertical = 7.dp).clip(RoundedCornerShape(12.dp)).background(Cream).clickable { selectedDay = d; note = text }.padding(12.dp)) { Text("روز $d", color = Gold, fontWeight = FontWeight.Bold); Text(text, maxLines = 4, fontSize = 13.sp) } }
            }
        }
    }

    @Composable private fun ReportsPage() {
        val totalTasks = (1..40).sumOf { dayDoneCount(it) }; val totalPossible = 40 * dailyTasks.size
        val habitDone = habitNames().indices.sumOf { h -> (0 until 400).count { prefs.getString("h400_${h}_$it", "") != "" } }
        val smallCount = habitNames().indices.sumOf { h -> (0 until 400).count { prefs.getString("h400_${h}_$it", "") == "s" } }
        val specialCount = habitNames().indices.sumOf { h -> (0 until 400).count { prefs.getString("h400_${h}_$it", "") == "v" } }
        val emergencyCount = habitNames().indices.sumOf { h -> (0 until 400).count { prefs.getString("h400_${h}_$it", "") == "e" } }
        val allCells = habitNames().size * 400
        ScrollPage {
            PremiumCard { SectionTitle("گزارش مسیر", "تصویری از استمرار و قدم‌های برداشته‌شده")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatTile("${if(allCells>0) habitDone * 100 / allCells else 0}%", "پیشرفت عادت‌ها", Modifier.weight(1f)); StatTile("${completedDays()}", "روز کامل چله", Modifier.weight(1f)); StatTile("${habitDone}", "خانه ثبت‌شده", Modifier.weight(1f)) }
                Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatTile("$smallCount", "لقمه کوچک", Modifier.weight(1f)); StatTile("$specialCount", "لقمه ویژه", Modifier.weight(1f)); StatTile("$emergencyCount", "لقمه اضطراری", Modifier.weight(1f)) }
                Spacer(Modifier.height(16.dp)); Text("پیشرفت چله", fontWeight = FontWeight.Bold, color = Forest); LinearProgressIndicator(progress = { completedDays() / 40f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(CircleShape), color = Gold, trackColor = PaleGold)
                Spacer(Modifier.height(14.dp)); Text("ثبت روزانه", fontWeight = FontWeight.Bold, color = Forest); LinearProgressIndicator(progress = { totalTasks / totalPossible.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(CircleShape), color = Forest2, trackColor = PaleGold)
            }
            PremiumCard { SectionTitle("گزارش روز به روز")
                (1..40).forEach { day -> val count = dayDoneCount(day); Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text("روز $day", Modifier.width(62.dp), fontSize = 12.sp, color = Muted); LinearProgressIndicator(progress = { count / dailyTasks.size.toFloat() }, modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape), color = if (count == dailyTasks.size) Forest else Gold, trackColor = PaleGold); Text("$count/${dailyTasks.size}", Modifier.width(48.dp), textAlign = TextAlign.End, fontSize = 11.sp) } }
            }
        }
    }

    @Composable private fun RemindersPage() {
        var enabled by remember { mutableStateOf(reminderEnabled) }; var hour by remember { mutableIntStateOf(reminderHour) }; var minute by remember { mutableIntStateOf(reminderMinute) }
        ScrollPage {
            PremiumCard { SectionTitle("یادآور بومی اندروید", "اعلان مستقل از باز بودن برنامه زمان‌بندی می‌شود")
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("یادآوری روزانهٔ عهد", fontWeight = FontWeight.Bold); Text("یک پیام برای بازگشت به نیت روزانه", color = Muted, fontSize = 12.sp) }; Switch(enabled, { enabled = it }) }
                Spacer(Modifier.height(16.dp)); Text("زمان یادآوری", color = Forest, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { IconButton(onClick = { hour = (hour + 23) % 24 }) { Icon(Icons.Filled.Remove, null) }; Text(String.format(Locale.US, "%02d", hour), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Forest); Text(":", fontSize = 30.sp); Text(String.format(Locale.US, "%02d", minute), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Forest); IconButton(onClick = { hour = (hour + 1) % 24 }) { Icon(Icons.Filled.Add, null) } }
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = { minute = (minute + 55) % 60 }) { Text("−۵ دقیقه") }; TextButton(onClick = { minute = (minute + 5) % 60 }) { Text("+۵ دقیقه") } }
                Spacer(Modifier.height(8.dp)); PrimaryButton("ذخیره و فعال‌سازی", { configureReminder(enabled,hour,minute); Toast.makeText(this@MainActivity, if (enabled) "یادآور ذخیره شد" else "یادآور غیرفعال شد", Toast.LENGTH_LONG).show() }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { showTestNotification() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.NotificationsActive, null); Spacer(Modifier.width(8.dp)); Text("آزمایش اعلان") }
                Spacer(Modifier.height(6.dp)); TextButton(onClick = { openNotificationSettings() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Settings, null); Spacer(Modifier.width(6.dp)); Text("بررسی مجوز و تنظیمات اعلان") }
            }
            PremiumCard { SectionTitle("نکتهٔ مهم")
                Text("برای نمایش اعلان، مجوز اعلان‌های برنامه را در تنظیمات اندروید فعال نگه دار. بعضی گوشی‌ها برای اجرای دقیق در پس‌زمینه، تنظیمات باتری جداگانه دارند.", fontSize = 13.sp, color = Muted)
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 30) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                showTestNotification()
            } else {
                Toast.makeText(this, "برای نمایش اعلان آزمایشی، مجوز اعلان را در تنظیمات برنامه فعال کن.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openNotificationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= 26) {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderReceiver.CHANNEL)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            }
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun showTestNotification() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 30)
            Toast.makeText(this, "برای آزمایش، مجوز اعلان را در پنجرهٔ اندروید فعال کن.", Toast.LENGTH_LONG).show()
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(ReminderReceiver.CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE)) {
            Toast.makeText(this, "اعلان‌های برنامه یا کانال آن خاموش است؛ تنظیمات اعلان باز می‌شود.", Toast.LENGTH_LONG).show()
            openNotificationSettings()
            return
        }
        val message = ReminderReceiver.MESSAGES[(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + 3) % ReminderReceiver.MESSAGES.size]
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, ReminderReceiver.CHANNEL) else Notification.Builder(this)
        val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("چله‌یار مهدوی · پیام امروز")
            .setContentText(message).setStyle(Notification.BigTextStyle().bigText(message)).setAutoCancel(true).build()
        manager.notify(2029, notification)
        Toast.makeText(this, "اعلان آزمایشی ارسال شد؛ اگر دیده نشد تنظیمات اعلان و مزاحم‌نشدن را بررسی کن.", Toast.LENGTH_LONG).show()
    }

    @Composable private fun SettingsPage() {
        ScrollPage {
            PremiumCard { SectionTitle("تنظیمات و پشتیبان‌گیری", "اطلاعات شخصی روی همین دستگاه نگهداری می‌شود")
                SettingRow(Icons.Filled.FileUpload, "خروجی پشتیبان", "ذخیرهٔ اطلاعات در فایل JSON") { exportLauncher.launch("chillehyar-backup.json") }
                Divider(color = PaleGold)
                SettingRow(Icons.Filled.FileDownload, "بازیابی پشتیبان", "انتخاب فایل JSON قبلی") { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }
                Divider(color = PaleGold)
                SettingRow(Icons.Filled.Notifications, "تنظیم یادآورها", "زمان‌بندی اعلان‌های روزانه") { page = "یادآورها" }
                Divider(color = PaleGold)
                SettingRow(Icons.Filled.Insights, "گزارش پیشرفت", "مشاهدهٔ روند ثبت‌ها") { page = "گزارش" }
            }
            PremiumCard { SectionTitle("شخصی‌سازی ظاهر", "رنگ و اندازهٔ نوشته‌ها را متناسب با چشم خود تنظیم کن")
                val colors = listOf(Color(0xFF173A31), Color(0xFF176B55), Color(0xFF2F6FBA), Color(0xFF7B61A8), Color(0xFFB96865), Color(0xFF946C2E))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) { colors.forEachIndexed { i,c -> Box(Modifier.size(38.dp).clip(CircleShape).background(c).border(if(themeChoice==i) 3.dp else 0.dp, Gold, CircleShape).clickable { themeChoice=i; prefs.edit().putInt("theme_choice",i).apply(); revision++ }) } }
                Spacer(Modifier.height(12.dp)); Text("اندازهٔ متن", color = Forest, fontWeight = FontWeight.Bold)
                Slider(value = uiScale, onValueChange = { uiScale=it }, valueRange = .9f..1.2f, steps = 5)
                Text("${(uiScale*100).toInt()}٪ · مقیاس نوشته‌ها در همین برنامه اعمال می‌شود.", color = Muted, fontSize = 11.sp)
                PrimaryButton("ذخیرهٔ ظاهر", { prefs.edit().putInt("theme_choice",themeChoice).putFloat("ui_scale",uiScale).apply(); Toast.makeText(this@MainActivity,"تنظیمات ظاهر ذخیره شد",Toast.LENGTH_SHORT).show() }, Modifier.fillMaxWidth())
            }
            PremiumCard { SectionTitle("دربارهٔ چله‌یار")
                Text("چله‌یار مهدوی · نسخهٔ بومی اندروید ۳۱ · پیشرفته", color = Forest, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp)); Text("برای همراهی در یک مسیر چهل‌روزه از عهد، مراقبت و رشد معنوی.", color = Muted, fontSize = 13.sp)
            }
        }
    }
    @Composable private fun ToolsPage() {
        ScrollPage {
            PremiumCard { SectionTitle("ابزارهای پیشرفتهٔ چله‌یار", "امکانات تکمیلی نسخهٔ ۲۹ در قالب رابط بومی اندروید")
                ToolRow("📋", "جدول الف و فضیلت‌ها", "ثبت روزانه در ۱۱ دسته", { page = "جدول فضیلت‌ها" })
                Divider(color = PaleGold)
                ToolRow("🌱", "چک‌لیست افزایش انگیزه", "۱۶ عامل انگیزشی با سابقهٔ روزانه", { page = "افزایش انگیزه" })
                Divider(color = PaleGold)
                ToolRow("🧭", "بررسی عادت و علت لغزش", "ثبت علت‌ها، راهکارها و مرور بررسی‌های قبلی", { page = "تحلیل لغزش" })
                Divider(color = PaleGold)
                ToolRow("📈", "گزارش و کارنامه", "خانه‌های کوچک، ویژه، اضطراری و پیشرفت هر عادت", { page = "گزارش" })
                Divider(color = PaleGold)
                ToolRow("🔔", "یادآورها", "یادآور روزانه و آزمایش اعلان اندروید", { page = "یادآورها" })
                Divider(color = PaleGold)
                ToolRow("🎨", "ظاهر و پشتیبان", "رنگ‌بندی، خروجی و بازیابی اطلاعات", { page = "تنظیمات" })
            }
            PremiumCard { SectionTitle("یادآوری دربارهٔ اطلاعات")
                Text("اطلاعات این نسخه در حافظهٔ محلی برنامه ذخیره می‌شود. برای انتقال به گوشی دیگر یا پیشگیری از ازدست‌رفتن داده‌ها، از بخش تنظیمات مرتباً نسخهٔ پشتیبان JSON بگیر.", color = Muted, fontSize = 13.sp, lineHeight = 22.sp)
            }
        }
    }
    @Composable private fun ToolRow(emoji: String, title: String, subtitle: String, action: () -> Unit) {
        Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(PaleGold), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 23.sp) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = Forest, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }; Icon(Icons.Filled.ChevronLeft, null, tint = Gold)
        }
    }

    @Composable private fun VirtuePage() {
        val dateKey = selectedVirtueDate.replace("/", "_").replace(" ", "")
        ScrollPage {
            PremiumCard { SectionTitle("جدول الف و فضیلت‌ها", "ثبت موضوعات روزانه در ۱۱ دسته")
                OutlinedTextField(selectedVirtueDate, { selectedVirtueDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("تاریخ (شمسی)") }, singleLine = true)
                Spacer(Modifier.height(8.dp)); Text("هر کادر را برای همان تاریخ تکمیل کن؛ اطلاعات خودکار ذخیره می‌شود.", fontSize = 12.sp, color = Muted)
            }
            virtueHeaders.forEachIndexed { i, label ->
                var value by remember(dateKey, i) { mutableStateOf(prefs.getString("virtue_${dateKey}_$i", "") ?: "") }
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(PaleGold), contentAlignment = Alignment.Center) { Text("${i+1}", color = Forest, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(9.dp)); Text(label, color = Forest, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(7.dp)); OutlinedTextField(value, { value = it; prefs.edit().putString("virtue_${dateKey}_$i",it).apply() }, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("ثبت روزانه در دستهٔ $label…") }, shape = RoundedCornerShape(12.dp))
                }
            }
            OutlinedButton(onClick = { virtueHeaders.indices.forEach { prefs.edit().remove("virtue_${dateKey}_$it").apply() }; revision++ }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("پاک‌کردن جدول این تاریخ") }
        }
    }

    @Composable private fun MotivationPage() {
        val dateKey = motivationDateDraft.replace("/", "_").replace(" ", "")
        val checked = remember(dateKey, revision) { motivationItems.indices.filter { prefs.getBoolean("mot_${dateKey}_$it", false) }.toSet() }
        var titleDraft by remember(dateKey) { mutableStateOf(prefs.getString("mot_title_$dateKey", motivationTitleDraft) ?: motivationTitleDraft) }
        ScrollPage {
            PremiumCard { SectionTitle("چک‌لیست افزایش انگیزه", "۱۶ عامل برای زنده نگه‌داشتن انگیزه")
                OutlinedTextField(titleDraft, { titleDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("عنوان ثبت") }, singleLine = true)
                Spacer(Modifier.height(8.dp)); OutlinedTextField(motivationDateDraft, { motivationDateDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("تاریخ ثبت (شمسی)") }, singleLine = true)
                Spacer(Modifier.height(10.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("پیشرفت امروز", color = Forest, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("${checked.size} از ${motivationItems.size}", color = Gold, fontWeight = FontWeight.Bold) }
                LinearProgressIndicator(progress = { checked.size / motivationItems.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(7.dp).clip(CircleShape), color = Gold, trackColor = PaleGold)
            }
            motivationItems.forEachIndexed { i, item ->
                val isChecked = prefs.getBoolean("mot_${dateKey}_$i", false)
                Row(Modifier.fillMaxWidth().padding(bottom = 7.dp).clip(RoundedCornerShape(14.dp)).background(if(isChecked) Color(0xFFE6F2E9) else CardCream).clickable { prefs.edit().putBoolean("mot_${dateKey}_$i",!isChecked).putString("mot_title_$dateKey",titleDraft).apply(); revision++ }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(isChecked, { prefs.edit().putBoolean("mot_${dateKey}_$i",it).putString("mot_title_$dateKey",titleDraft).apply(); revision++ }, colors = CheckboxDefaults.colors(checkedColor = Forest)); Text(item, Modifier.weight(1f), color = if(isChecked) Forest else Ink, fontSize = 13.sp); if(isChecked) Icon(Icons.Filled.CheckCircle, null, tint = Forest)
                }
            }
            PrimaryButton("ثبت چک‌لیست این روز در سابقه", { prefs.edit().putString("mot_title_$dateKey",titleDraft).putString("mot_saved_${dateKey}",checked.joinToString(",")).apply(); Toast.makeText(this@MainActivity,"چک‌لیست ذخیره شد",Toast.LENGTH_SHORT).show(); revision++ }, Modifier.fillMaxWidth())
            PremiumCard { SectionTitle("سابقهٔ ثبت‌ها")
                val history = prefs.all.keys.filter { it.startsWith("mot_saved_") }.sortedDescending()
                if(history.isEmpty()) Text("هنوز سابقه‌ای ثبت نشده است.", color = Muted) else history.forEach { key ->
                    val k = key.removePrefix("mot_saved_"); val title = prefs.getString("mot_title_$k", "چک‌لیست انگیزشی") ?: "چک‌لیست انگیزشی"; val count = (prefs.getString(key, "") ?: "").split(",").count { it.isNotBlank() }
                    Text("$title · ${k.replace("_", "/")} · $count مورد", modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), color = Forest, fontSize = 12.sp)
                }
            }
        }
    }

    @Composable private fun TacticsPage() {
        ScrollPage {
            PremiumCard { SectionTitle("بررسی عادت و علت لغزش", "هدف سرزنش نیست؛ پیدا کردن یک قدم بهتر است")
                OutlinedTextField(tacticNameDraft, { tacticNameDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام عادت یا موقعیت") }, singleLine = true)
                Spacer(Modifier.height(10.dp)); Text("کدام علت‌ها نقش داشتند؟", color = Forest, fontWeight = FontWeight.Bold)
                tacticCauses.forEachIndexed { i, cause -> Row(Modifier.fillMaxWidth().clickable { tacticCauseDraft = if(i in tacticCauseDraft) tacticCauseDraft - i else tacticCauseDraft + i }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(i in tacticCauseDraft, { tacticCauseDraft = if(it) tacticCauseDraft + i else tacticCauseDraft - i }, colors = CheckboxDefaults.colors(checkedColor = Forest)); Text(cause, fontSize = 13.sp) } }
                Spacer(Modifier.height(8.dp)); OutlinedTextField(tacticSolutionDraft, { tacticSolutionDraft = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("راهکار من برای دفعهٔ بعد") })
                Spacer(Modifier.height(9.dp)); PrimaryButton("ذخیرهٔ این بررسی", {
                    if(tacticNameDraft.isBlank() || tacticSolutionDraft.isBlank()) Toast.makeText(this@MainActivity,"نام عادت و راهکار را وارد کن",Toast.LENGTH_SHORT).show()
                    else { val id = System.currentTimeMillis(); prefs.edit().putString("tactic_$id", JSONObject().put("name",tacticNameDraft).put("causes",JSONArray(tacticCauseDraft.sorted().map { tacticCauses[it] })).put("solution",tacticSolutionDraft).put("date",SimpleDateFormat("yyyy/MM/dd",Locale.getDefault()).format(Date())).toString()).apply(); tacticNameDraft=""; tacticSolutionDraft=""; tacticCauseDraft= emptySet(); revision++; Toast.makeText(this@MainActivity,"بررسی ذخیره شد",Toast.LENGTH_SHORT).show() }
                }, Modifier.fillMaxWidth())
            }
            PremiumCard { SectionTitle("بررسی‌های ذخیره‌شده")
                val records = prefs.all
                    .filterKeys { it.startsWith("tactic_") }
                    .toList()
                    .sortedByDescending { it.first }
                    .mapNotNull { (key, raw) ->
                        val json = raw as? String ?: return@mapNotNull null
                        val parsed = try { JSONObject(json) } catch (_: Exception) { null }
                        parsed?.let { key to it }
                    }
                if (records.isEmpty()) Text("هنوز بررسی‌ای ثبت نشده است.", color = Muted)
                records.forEach { (key, o) ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(13.dp)).background(Cream).padding(12.dp)) {
                        Text(o.optString("name"), color = Forest, fontWeight = FontWeight.Bold)
                        Text("${o.optString("date")} · ${o.optJSONArray("causes")?.length() ?: 0} علت", color = Muted, fontSize = 11.sp)
                        Text(o.optString("solution"), fontSize = 13.sp, lineHeight = 20.sp)
                        TextButton(onClick = { prefs.edit().remove(key).apply(); revision++ }) { Text("حذف بررسی", color = Rose) }
                    }
                }
            }
        }
    }

    @Composable private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(PaleGold), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Forest) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 11.sp) }; Icon(Icons.Filled.ChevronLeft, null, tint = Gold) } }
}

class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("chillehyar_native_v30", Context.MODE_PRIVATE)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "یادآورهای چله‌یار", NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (intent?.action == "ir.chillehyar.mahdi.HABIT_REMINDER") {
            val index = intent.getIntExtra("habit_index",0)
            val name = intent.getStringExtra("habit_name") ?: "عادت روزانه"
            val message = intent.getStringExtra("habit_message")?.takeIf { it.isNotBlank() } ?: MESSAGES[(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + index) % MESSAGES.size]
            val time = intent.getStringExtra("habit_time") ?: "20:00"
            val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL) else Notification.Builder(context)
            manager.notify(10000 + index, b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("چله‌یار · $name").setContentText(message).setAutoCancel(true).build())
            scheduleHabit(context,index,name,time,message)
            return
        }
        if (!prefs.getBoolean("reminder_enabled", false)) return
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL) else Notification.Builder(context)
        val message = MESSAGES[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % MESSAGES.size]
        manager.notify(4002, b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("چله‌یار مهدوی · یادآوری عهد").setContentText(message).setStyle(Notification.BigTextStyle().bigText(message)).setAutoCancel(true).build())
        schedule(context, prefs)
    }
    companion object {
        const val CHANNEL = "chillehyar_native_v32"
        const val REQUEST_CODE = 730
        val MESSAGES = listOf(
            "امروز را با سلامی به امام زمان (عج) آغاز کن؛ یک قدم کوچک کافی است.",
            "اللهم عجل لولیک الفرج؛ امروز یک کار خوب را به نیت رضایت خدا انجام بده.",
            "یار امام زمان بودن از یک انتخاب پاک در همین لحظه آغاز می‌شود.",
            "آیا امروز برای فرج دعا کردی؟ چند لحظه با دل حاضر دعا کن.",
            "امروز دل کسی را شاد کن؛ شاید همین قدم کوچک، عهدی روشن باشد.",
            "برای تعجیل فرج دعا کن و سهم خودت از اصلاح را فراموش نکن.",
            "یک صلوات، یک دعای فرج، یک کار نیک؛ عهد امروزت را زنده نگه دار.",
            "امام زمان (عج) را با عمل صالح به یاد بیاور؛ قدمی هرچند کوچک بردار.",
            "خدایا ما را از یاران راستین و زمینه‌سازان فرج قرار بده.",
            "امروز با مهربانی، امانت‌داری و دوری از گناه به عهدت وفا کن.",
            "دعای عهد را با توجه بخوان؛ امید را در رفتار امروزت جاری کن.",
            "فرج را بخواه و برای بهتر شدن خودت هم یک تصمیم عملی بگیر.",
            "هر قدم صادقانه در مسیر بندگی ارزشمند است؛ ناامید نشو.",
            "به نیت تعجیل فرج، به خانواده‌ات محبت کن و حق کسی را رعایت کن.",
            "سلام بر امام زمان (عج)؛ امروز را با نیت پاک و امید آغاز کن."
        )
        fun schedule(context: Context, prefs: android.content.SharedPreferences) {
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pi = PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (!prefs.getBoolean("reminder_enabled", false)) { alarm.cancel(pi); return }
            val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, prefs.getInt("reminder_hour", 8)); set(Calendar.MINUTE, prefs.getInt("reminder_minute", 0)); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
            if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi) else alarm.set(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
        }
        fun scheduleHabit(context: Context, index: Int, name: String, time: String, message: String) {
            val p = context.getSharedPreferences("chillehyar_native_v30", Context.MODE_PRIVATE)
            if (!p.getBoolean("habit_reminder_${index}_on", false)) return
            val parts = time.split(":"); val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0,23) ?: 20; val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0,59) ?: 0
            val intent = Intent(context, ReminderReceiver::class.java).apply { action = "ir.chillehyar.mahdi.HABIT_REMINDER"; putExtra("habit_index",index); putExtra("habit_name",name); putExtra("habit_time",time); putExtra("habit_message",message) }
            val pi = PendingIntent.getBroadcast(context,10000+index,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,hour); set(Calendar.MINUTE,minute); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0); if(timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1) }
            val alarm = context.getSystemService(AlarmManager::class.java)
            if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi) else alarm.set(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi)
        }
    }
}

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("chillehyar_native_v30", Context.MODE_PRIVATE)
            ReminderReceiver.schedule(context, prefs)
            val defaults = listOf("قرائت قرآن", "خوش‌اخلاقی", "دعا برای فرج")
            val extras = (prefs.getString("custom_habits", "") ?: "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            (defaults + extras).forEachIndexed { index, name ->
                if (prefs.getBoolean("habit_reminder_${index}_on", false)) ReminderReceiver.scheduleHabit(context,index,name,prefs.getString("habit_reminder_${index}_time","20:00") ?: "20:00",prefs.getString("habit_reminder_${index}_text","") ?: "یک قدم کوچک امروز هم ارزشمند است.")
            }
        }
    }
}
