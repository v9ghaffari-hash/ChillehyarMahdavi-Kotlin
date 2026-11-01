package ir.chillehyar.mahdi

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val tabs = arrayOf("خانه", "چلهٔ من", "عادت‌ها", "ثبت روزانه", "یادداشت‌ها", "گزارش", "یادآورها", "تنظیمات")
    private val habits = arrayOf("نماز اول وقت", "قرائت قرآن", "صلوات و ذکر", "ترک یک گناه", "خدمت و مهربانی")
    private val defaultTasks = arrayOf(
        arrayOf("نماز صبح", "نماز ظهر", "نماز عصر", "نماز مغرب", "نماز عشا", "نماز اول وقت", "حضور قلب", "تعقیبات", "نماز مستحبی", "محاسبهٔ نماز"),
        arrayOf("قرآن صبحگاهی", "ترجمهٔ آیات", "تدبر در آیات", "حفظ یک آیه", "تلاوت روزانه", "سورهٔ عهد", "آیت‌الکرسی", "هدیهٔ ثواب", "عمل به یک آیه", "مرور آموخته‌ها"),
        arrayOf("صلوات روزانه", "دعای فرج", "دعای عهد", "ذکر استغفار", "ذکر یا مهدی", "صدقه برای سلامتی", "یاد امام زمان", "زیارت آل‌یاسین", "دعا برای مؤمنان", "تجدید عهد"),
        arrayOf("کنترل زبان", "کنترل نگاه", "ترک غیبت", "ترک دروغ", "کنترل خشم", "ترک اسراف", "کنترل فضای مجازی", "دوری از بدگمانی", "حفظ حق‌الناس", "توبه و جبران"),
        arrayOf("کمک به خانواده", "احوال‌پرسی", "دستگیری نیازمند", "لبخند و خوش‌رفتاری", "آموزش یا انتقال خیر", "رسیدگی به والدین", "رفع کدورت", "کار خیر پنهانی", "رعایت حق دیگران", "نیت خدمت")
    )
    private var activeHabit = 0
    private fun habitNames(): List<String> = habits.toList() + listOfNotNull(getValue("custom_habit").takeIf { it.isNotBlank() })
    private fun defaultTask(habit: Int, task: Int): String = defaultTasks.getOrNull(habit)?.getOrNull(task) ?: "ریزکار ${task + 1}"
    private val daily = arrayOf("نمازهایم را با توجه خواندم", "قرآن و دعا خواندم", "برای امام زمان(عج) دعا کردم", "از گناه و عادت نامطلوب دوری کردم", "به دیگران نیکی کردم", "محاسبهٔ نفس انجام دادم")
    private lateinit var pref: android.content.SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private var font: Typeface? = null
    private var player: MediaPlayer? = null
    private val green = 0xff26382f.toInt()
    private val gold = 0xffb58a45.toInt()
    private val paper = 0xfff7f4ec.toInt()
    private val ink = 0xff26332c.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("chillehyar_native", MODE_PRIVATE)
        font = runCatching { Typeface.createFromAsset(assets, "fonts/BNazanin.ttf") }.getOrNull()
        createChannel()
        showTab("خانه")
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("chillehyar_native", "یادآورهای چله‌یار", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "یادآوری‌های برنامهٔ چله‌یار مهدوی"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun text(value: String, size: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(ink); gravity = Gravity.RIGHT
        setPadding(8, 8, 8, 8)
        typeface = font?.let { Typeface.create(it, if (bold) Typeface.BOLD else Typeface.NORMAL) }
            ?: Typeface.create(typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    private fun showTab(tab: String) {
        root = column().apply { layoutDirection = View.LAYOUT_DIRECTION_RTL; setBackgroundColor(paper) }
        setContentView(root)
        val head = text("چله‌یار مهدوی  ✦", 23, true).apply {
            setTextColor(-1); setBackgroundColor(green); gravity = Gravity.CENTER; setPadding(12, 18, 12, 18)
        }
        root.addView(head)
        val scroll = ScrollView(this)
        body = column().apply { setPadding(14, 10, 14, 24) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        when {
            tab.startsWith("جزئیات|") -> habitDetail(tab.substringAfter("|"))
            tab == "خانه" -> home(); tab == "چلهٔ من" -> chilla(); tab == "عادت‌ها" -> habitsPage(); tab == "ثبت روزانه" -> dailyPage()
            tab == "یادداشت‌ها" -> notesPage(); tab == "گزارش" -> report(); tab == "یادآورها" -> reminders(); else -> settings()
        }
        val navigationScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setBackgroundColor(green) }
        val navigation = row().apply { setBackgroundColor(green) }
        tabs.forEach { item ->
            val navButton = Button(this).apply { text = item; textSize = 12f; isAllCaps = false; setTextColor(-1); setBackgroundColor(green); setPadding(14, 10, 14, 10); setOnClickListener { showTab(item) } }
            navigation.addView(navButton, LinearLayout.LayoutParams(-2, -2))
        }
        navigationScroll.addView(navigation)
        root.addView(navigationScroll)
    }

    private fun button(label: String) = Button(this).apply {
        text = label; isAllCaps = false; setTextColor(-1); backgroundTintList = ColorStateList.valueOf(gold)
    }
    private fun title(value: String) { body.addView(text(value, 20, true).apply { setTextColor(green); setPadding(4, 18, 4, 8) }) }
    private fun para(value: String) { body.addView(text(value, 16, false)) }
    private fun card(value: String) { body.addView(text(value, 16, false).apply { setBackgroundColor(-1); setPadding(14, 14, 14, 14) }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 5, 0, 5) }) }
    private fun edit(hintText: String, value: String, lines: Int): EditText = EditText(this).apply {
        hint = hintText; setText(value); textSize = 16f; gravity = Gravity.RIGHT or Gravity.TOP; minLines = lines
        setBackgroundColor(-1); setPadding(12, 10, 12, 10); body.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun save(key: String, value: String) { pref.edit().putString(key, value).apply() }
    private fun getValue(key: String, default: String = "") = pref.getString(key, default) ?: default
    private fun isChecked(key: String) = pref.getBoolean(key, false)
    private fun checkRow(label: String, key: String) {
        val check = CheckBox(this).apply {
            text = label; textSize = 16f; gravity = Gravity.RIGHT; buttonTintList = ColorStateList.valueOf(gold)
            isChecked = isChecked(key)
            setOnCheckedChangeListener { _, checked -> pref.edit().putBoolean(key, checked).apply() }
        }
        body.addView(check)
    }

    private fun home() {
        title("به نام خداوند مهربان")
        card("قدم‌های کوچک، استمرار بزرگ؛ این مسیر برای نزدیک‌تر شدن دل به امام زمان(عج) است.")
        val done = (0 until 40).count { pref.getBoolean("d$it", false) }
        title("پیشرفت چله"); para("$done روز از ۴۰ روز ثبت شده است.")
        body.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 40; progress = done })
        title("پیام امروز")
        card(getValue("daily_quote", "هر روز فرصتی تازه برای ساختن خویشتن و عهدی دوباره با امام زمان(عج) است."))
        title("شروع سریع")
        arrayOf("چلهٔ من", "عادت‌ها", "ثبت روزانه", "یادآورها").forEach { destination ->
            body.addView(button("ورود به $destination").apply { setOnClickListener { showTab(destination) } })
        }
    }
    private fun chilla() {
        title("چلهٔ چهل‌روزه"); para("هر روز پس از انجام برنامه، روز مربوط را علامت بزنید. ثبت‌ها روی همین دستگاه نگهداری می‌شوند.")
        body.addView(button("تعیین تاریخ شروع چله").apply { setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this@MainActivity, { _, y, m, d ->
                val chosen = Calendar.getInstance().apply { set(y, m, d) }
                save("start_date", SimpleDateFormat("yyyy/MM/dd", Locale("fa")).format(chosen.time))
                toast("تاریخ شروع ذخیره شد: ${getValue("start_date")}")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        } })
        para("تاریخ شروع: ${getValue("start_date", "هنوز تعیین نشده")}")
        repeat(40) { index ->
            body.addView(CheckBox(this).apply {
                text = "روز ${index + 1}"; textSize = 16f; isChecked = pref.getBoolean("d$index", false)
                setOnCheckedChangeListener { _, checked -> pref.edit().putBoolean("d$index", checked).apply() }
            })
        }
        title("هدف و نیت من")
        val input = edit("نیت و هدف این دوره را بنویسید", getValue("intention"), 3)
        body.addView(button("ذخیرهٔ نیت").apply { setOnClickListener { save("intention", input.text.toString()); toast("نیت ذخیره شد") } })
    }
    private fun todayKey(prefix: String) = prefix + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    private fun habitsPage() {
        title("عادت‌ها و جدول ۴۰ روزه")
        para("هر عادت صفحهٔ اختصاصی دارد. برای هر یک از ۱۰ ریزکار، وضعیت ۴۰ روز را ثبت کنید؛ روی هر خانه بزنید تا علامت آن تغییر کند.")
        habitNames().forEachIndexed { index, habit ->
            val completed = countHabit(index)
            val cardView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 12); setBackgroundColor(-1) }
            cardView.addView(text(habit, 18, true).apply { setTextColor(green) })
            cardView.addView(text("${completed.first} ثبت از ۴۰۰ خانه  •  پیشرفت ${completed.second}%", 14, false))
            cardView.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = completed.second })
            cardView.addView(button("باز کردن جدول این عادت").apply { setOnClickListener { activeHabit = index; showTab("جزئیات|$index") } })
            body.addView(cardView, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 7, 0, 7) })
        }
        title("افزودن عادت شخصی")
        val custom = edit("نام عادت دلخواه", getValue("custom_habit"), 1)
        body.addView(button("ذخیرهٔ عادت شخصی").apply { setOnClickListener { save("custom_habit", custom.text.toString()); toast("عادت ذخیره شد"); showTab("عادت‌ها") } })
        getValue("custom_habit").takeIf { it.isNotBlank() }?.let { card("عادت شخصی: $it؛ برای افزودن جدول اختصاصی، در نسخهٔ بعدی قابل توسعه است.") }
    }

    private fun cellKey(habit: Int, task: Int, day: Int) = "cell_${habit}_${task}_${day}"
    private fun countHabit(habit: Int): Pair<Int, Int> {
        var count = 0
        for (task in 0 until 10) for (day in 0 until 40) if (pref.getInt(cellKey(habit, task, day), 0) != 0) count++
        return count to (count * 100 / 400)
    }
    private fun habitDetail(rawIndex: String) {
        val names = habitNames()
        val index = rawIndex.toIntOrNull()?.coerceIn(0, names.lastIndex.coerceAtLeast(0)) ?: 0
        activeHabit = index
        title(names[index])
        para("جدول ۱۰ ریزکار × ۴۰ روز؛ هر خانه را لمس کنید تا بین سه نوع ثبت و حالت خالی جابه‌جا شود.")
        val counts = intArrayOf(0, 0, 0, 0)
        for (task in 0 until 10) for (day in 0 until 40) counts[pref.getInt(cellKey(index, task, day), 0).coerceIn(0, 3)]++
        card("✅ لقمه کوچک: ${counts[1]}     🚫 لقمه ویژه: ${counts[2]}     ❌ لقمه اضطراری: ${counts[3]}     خالی: ${counts[0]}")
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = true }
        val table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 4, 4, 4) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(text("ریزکار", 13, true).apply { setBackgroundColor(0xffe8dfcc.toInt()) }, LinearLayout.LayoutParams(130, -2))
        for (day in 1..40) header.addView(text(day.toString(), 11, true).apply { gravity = Gravity.CENTER; setBackgroundColor(0xffe8dfcc.toInt()) }, LinearLayout.LayoutParams(42, -2))
        table.addView(header)
        for (task in 0 until 10) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val taskName = getValue("task_${index}_$task", defaultTask(index, task))
            line.addView(text(taskName, 12, true).apply { minimumHeight = 42; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(0xfff0eadf.toInt()) }, LinearLayout.LayoutParams(130, -2))
            for (day in 0 until 40) {
                val key = cellKey(index, task, day)
                val value = pref.getInt(key, 0)
                val labels = arrayOf("·", "✅", "🚫", "❌")
                val colors = intArrayOf(0xffeeeae2.toInt(), 0xffdcebdc.toInt(), 0xffffedc7.toInt(), 0xfff4dada.toInt())
                val cell = Button(this).apply {
                    text = labels[value.coerceIn(0, 3)]; textSize = 10f; minimumWidth = 0; minimumHeight = 0
                    setPadding(0, 0, 0, 0); backgroundTintList = ColorStateList.valueOf(colors[value.coerceIn(0, 3)])
                    setOnClickListener {
                        val next = (pref.getInt(key, 0) + 1) % 4
                        pref.edit().putInt(key, next).apply()
                        text = labels[next]; backgroundTintList = ColorStateList.valueOf(colors[next])
                    }
                }
                line.addView(cell, LinearLayout.LayoutParams(40, 42).apply { setMargins(1, 1, 1, 1) })
            }
            table.addView(line)
        }
        horizontal.addView(table)
        body.addView(horizontal, LinearLayout.LayoutParams(-1, -2))
        title("ویرایش نام ۱۰ ریزکار")
        for (task in 0 until 10) {
            val field = edit("ریزکار ${task + 1}", getValue("task_${index}_$task", defaultTask(index, task)), 1)
            body.addView(button("ذخیرهٔ ریزکار ${task + 1}").apply { setOnClickListener { save("task_${index}_$task", field.text.toString()); toast("نام ریزکار ذخیره شد") } })
        }
        body.addView(button("بازگشت به فهرست عادت‌ها").apply { setOnClickListener { showTab("عادت‌ها") } })
    }
    private fun dailyPage() {
        title("محاسبهٔ نفس روزانه"); para("با صداقت و بدون سرزنش، وضعیت امروز را ثبت کنید.")
        val key = todayKey("daily")
        daily.forEachIndexed { index, item -> checkRow(item, key + index) }
        title("حال و ارزیابی امروز")
        val input = edit("امروز چه چیزی خوب پیش رفت؟ چه چیزی نیاز به اصلاح دارد؟", getValue(key + "note"), 4)
        body.addView(button("ذخیرهٔ ثبت روزانه").apply { setOnClickListener { save(key + "note", input.text.toString()); toast("ثبت روزانه ذخیره شد") } })
    }
    private fun notesPage() {
        title("یادداشت‌ها و عهدها")
        val input = edit("یادداشت، دعا، عهد یا برنامهٔ فردا...", getValue("notes"), 10)
        body.addView(button("ذخیرهٔ یادداشت").apply { setOnClickListener { save("notes", input.text.toString()); toast("یادداشت ذخیره شد") } })
        title("تاکتیک‌های مقابله با وسوسه")
        listOf("موقعیت محرک را ترک کن.", "چند دقیقه مکث و تنفس آرام داشته باش.", "جایگزین سالم و از پیش تعیین‌شده انجام بده.", "از خدا کمک بخواه و با یک فرد امین صحبت کن.", "پس از لغزش، سریع و بدون ناامیدی به مسیر برگرد.").forEach { card("• $it") }
    }
    private fun report() {
        title("کارنامهٔ پیشرفت")
        val days = (0 until 40).count { pref.getBoolean("d$it", false) }
        para("روزهای تکمیل‌شده: $days از ۴۰")
        body.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 40; progress = days })
        title("پیشرفت عادت‌ها")
        habitNames().forEachIndexed { index, habit ->
            val progress = countHabit(index)
            para("$habit: ${progress.first} از ۴۰۰ خانه (${progress.second}٪)")
            body.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; this.progress = progress.second })
        }
        title("ثبت‌های امروز")
        val key = todayKey("daily")
        val count = daily.indices.count { pref.getBoolean(key + it, false) }
        para("$count از ${daily.size} مورد روزانه انجام شده است.")
        title("یادآوری مهم"); card("این گزارش برای مشاهدهٔ روند شخصی است، نه قضاوت دربارهٔ ارزش شما. استمرار مهم‌تر از بی‌نقص بودن است.")
        body.addView(button("تهیهٔ پشتیبان متنی").apply { setOnClickListener { backup() } })
    }
    private fun reminders() {
        title("یادآور روزانه"); para("زمان یادآوری را انتخاب کنید. برای نمایش اعلان، مجوز اعلان اندروید لازم است.")
        body.addView(button("انتخاب ساعت یادآوری").apply {
            setOnClickListener { val now = Calendar.getInstance(); TimePickerDialog(this@MainActivity, { _, hour, minute -> schedule(hour, minute) }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show() }
        })
        body.addView(button("لغو یادآور").apply { setOnClickListener { getSystemService(AlarmManager::class.java).cancel(alarmIntent()); toast("یادآور لغو شد") } })
        body.addView(button("ارسال اعلان آزمایشی").apply { setOnClickListener { notifyNow() } })
        body.addView(button("انتخاب صدای دلخواه").apply {
            setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "audio/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 77) }
        })
        body.addView(button("پخش صدای انتخاب‌شده").apply { setOnClickListener { playAudio() } })
    }
    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(this, 71, Intent(this, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun schedule(hour: Int, minute: Int) {
        pref.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
        val time = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
        scheduleNext(time.timeInMillis)
        toast("یادآور روزانه تنظیم شد")
    }
    private fun scheduleNext(atMillis: Long) {
        val alarm = getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent())
            else if (Build.VERSION.SDK_INT >= 23) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent())
            else alarm.setExact(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent())
        } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent()) }
    }
    private fun notifyNow() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 9); return
        }
        val notification = Notification.Builder(this, "chillehyar_native").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("چله‌یار مهدوی").setContentText("وقت عهد امروز و یک قدم به سوی بهتر شدن است.").setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(4001, notification)
        toast("اعلان آزمایشی ارسال شد")
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 9 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) notifyNow()
        else if (requestCode == 9) toast("مجوز اعلان داده نشد؛ از تنظیمات گوشی فعالش کنید")
    }
    private fun settings() {
        title("تنظیمات برنامه"); para("اطلاعات شما به‌صورت محلی روی همین دستگاه ذخیره می‌شود.")
        body.addView(button("پشتیبان‌گیری و اشتراک‌گذاری اطلاعات").apply { setOnClickListener { backup() } })
        body.addView(button("بازیابی از متن پشتیبان").apply {
            setOnClickListener {
                val input = EditText(this@MainActivity).apply { hint = "متن پشتیبان را وارد کنید" }
                AlertDialog.Builder(this@MainActivity).setTitle("بازیابی اطلاعات").setView(input)
                    .setPositiveButton("بازیابی") { _, _ -> restore(input.text.toString()) }.setNegativeButton("انصراف", null).show()
            }
        })
        body.addView(button("پاک‌کردن همهٔ اطلاعات").apply {
            setOnClickListener { AlertDialog.Builder(this@MainActivity).setMessage("همهٔ اطلاعات ذخیره‌شده پاک شود؟")
                .setPositiveButton("پاک شود") { _, _ -> pref.edit().clear().apply(); showTab("خانه") }.setNegativeButton("انصراف", null).show() }
        })
    }
    private fun backup() {
        runCatching {
            val json = JSONObject()
            pref.all.forEach { (key, value) -> json.put(key, value) }
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, json.toString()) }, "ذخیره یا ارسال پشتیبان"))
        }.onFailure { toast("تهیهٔ پشتیبان ناموفق بود") }
    }
    private fun restore(raw: String) {
        runCatching {
            val json = JSONObject(raw); val editor = pref.edit()
            json.keys().forEach { key -> when (val value = json.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Number -> editor.putLong(key, value.toLong())
                else -> editor.putString(key, value.toString())
            } }
            editor.apply(); toast("بازیابی انجام شد"); showTab("خانه")
        }.onFailure { toast("متن پشتیبان معتبر نیست") }
    }
    @Deprecated("Deprecated in Android, retained for document picker compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 77 && resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            save("audio", uri.toString()); toast("فایل صوتی انتخاب شد")
        }
    }
    private fun playAudio() {
        val uri = getValue("audio")
        if (uri.isBlank()) { toast("ابتدا فایل صوتی انتخاب کنید"); return }
        runCatching {
            player?.release(); player = MediaPlayer().apply {
                setDataSource(this@MainActivity, Uri.parse(uri)); setOnPreparedListener { it.start() }; prepareAsync()
            }
        }.onFailure { toast("پخش فایل صوتی ممکن نشد") }
    }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
