package com.simplemobiletools.keyboard.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.TypedValue
import android.util.Xml
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
import android.widget.Toast
import androidx.annotation.XmlRes
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.extensions.config
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard consists of rows of keys.
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 */
class MyKeyboard {
    /** Horizontal gap default for all rows  */
    private var mDefaultHorizontalGap = 0

    /** Default key width  */
    private var mDefaultWidth = 0

    /** Default key height  */
    private var mDefaultHeight = 0

    /** Multiplier for the keyboard height */
    var mKeyboardHeightMultiplier: Float = 1F

    /** Is the keyboard in the shifted state  */
    var mShiftState = SHIFT_OFF

    /** Total height of the keyboard, including the padding and keys  */
    var mHeight = 0

    /** Total width of the keyboard, including left side gaps and keys, but not any gaps on the right side. */
    var mMinWidth = 0

    /** List of keys in this keyboard  */
    var mKeys: MutableList<Key?>? = null

    /** Width of the screen available to fit the keyboard  */
    private var mDisplayWidth = 0

    /** What icon should we show at Enter key  */
    private var mEnterKeyType = IME_ACTION_NONE

    /** Keyboard rows  */
    private val mRows = ArrayList<Row?>()

    companion object {
        private const val TAG_KEYBOARD = "Keyboard"
        private const val TAG_ROW = "Row"
        private const val TAG_KEY = "Key"
        private const val EDGE_LEFT = 0x01
        private const val EDGE_RIGHT = 0x02
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_MODE_CHANGE = -2
        const val KEYCODE_ENTER = -4
        const val KEYCODE_DELETE = -5
        const val KEYCODE_SPACE = 32
        const val KEYCODE_EMOJI = -6
        const val KEYCODE_KEYBOARD_CHANGE = -3

        fun getDimensionOrFraction(a: TypedArray, index: Int, base: Int, defValue: Int): Int {
            val value = a.peekValue(index) ?: return defValue
            return when (value.type) {
                TypedValue.TYPE_DIMENSION -> a.getDimensionPixelOffset(index, defValue)
                TypedValue.TYPE_FRACTION -> a.getFraction(index, base, base, defValue.toFloat()).roundToInt()
                else -> defValue
            }
        }

        fun getRMSLattr(RMSLstr: String, attrName: String, default: String?): String? {
            val start = RMSLstr.indexOf("\t$attrName ") + "\t$attrName ".length
            val end = RMSLstr.indexOf(" $attrName\t")
            return if (start == -1 || end == -1) default else RMSLstr.substring(start, end)
        }

        fun removeRMSLattr(RMSLstr: String, attrName: String): String {
            return RMSLstr.substring(RMSLstr.indexOf(" $attrName\t") + " $attrName\t".length)
        }

        fun getRMSLattrs(RMSLstr: String, attrName: String): ArrayList<String> {
            var inProgressRMSLstr: String = RMSLstr

            val out = ArrayList<String>()
            var now: String
            while (true) {
                now = getRMSLattr(inProgressRMSLstr, attrName, null) ?: break
                out.add(now)
                inProgressRMSLstr = removeRMSLattr(inProgressRMSLstr, attrName)
            }

            return out
        }

        /** The default keyboard if none can be loaded  */
        const val DEFAULT_KEYBOARD = "\tkeyboardKeyWidth 10 keyboardKeyWidth\t\trow \t\tkeyboardMode0 true keyboardMode0\t\t\tkey \t\t\tkeyEdgeFlags left keyEdgeFlags\t\t\t\tkeyLabel q keyLabel\t\t\t\tpopupCharacters 1 popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 1 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel w keyLabel\t\t\t\tpopupCharacters 2 popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 2 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel e keyLabel\t\t\t\tpopupCharacters éè3êëēę popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 3 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel r keyLabel\t\t\t\tpopupCharacters ř4ŕ popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 4 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel t keyLabel\t\t\t\tpopupCharacters 5ť popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 5 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel y keyLabel\t\t\t\tpopupCharacters ý6ÿ¥ popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 6 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel u keyLabel\t\t\t\tpopupCharacters űúù7üûū popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 7 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel i keyLabel\t\t\t\tpopupCharacters íìî8ïī popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 8 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyLabel o keyLabel\t\t\t\tpopupCharacters őóôòõō9ö popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 9 topSmallNumber\t\t\t key\t\t\tkey \t\t\tkeyEdgeFlags right keyEdgeFlags\t\t\t\tkeyLabel p keyLabel\t\t\t\tpopupCharacters 0 popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t\ttopSmallNumber 0 topSmallNumber\t\t\t key\t\t row\t\trow \t\tkeyboardMode0 true keyboardMode0\t\t\tkey \t\t\thGap% 5 hGap%\t\t\t\tkeyEdgeFlags left keyEdgeFlags\t\t\t\tkeyLabel a keyLabel\t\t\t\tpopupCharacters áäàâãåāæą popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t\tkey \t\t\tkeyLabel s keyLabel\t\t\t\tpopupCharacters śßš popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t\tkey \t\t\tkeyLabel d keyLabel\t\t\t\tpopupCharacters ďđ popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t\tkey \t\t\tkeyLabel f keyLabel\t\t\t\tpopupCharacters ₣ popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t\tkey \t\t\tkeyLabel g keyLabel\t\t\t key\t\t\tkey \t\t\tkeyLabel h keyLabel\t\t\t key\t\t\tkey \t\t\tkeyLabel j keyLabel\t\t\t key\t\t\tkey \t\t\tkeyLabel k keyLabel\t\t\t key\t\t\tkey \t\t\tkeyEdgeFlags right keyEdgeFlags\t\t\t\tkeyLabel l keyLabel\t\t\t\tpopupCharacters ĺľł popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t row\t\trow \t\tkeyboardMode0 true keyboardMode0\t\t\tkey \t\t\tcode -1 code\t\t\t\tkeyEdgeFlags left keyEdgeFlags\t\t\t\tkeyIcon ic_caps_outline_vector keyIcon\t\t\t\tkeyWidth 15 keyWidth\t\t\t key\t\t\tkey \t\t\tkeyLabel z keyLabel\t\t\t\tpopupCharacters źžż popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t\tkey \t\t\tkeyLabel x keyLabel\t\t\t key\t\t\tkey \t\t\tkeyLabel c keyLabel\t\t\t\tpopupCharacters çčć¢ popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t\tkey \t\t\tkeyLabel v keyLabel\t\t\t key\t\t\tkey \t\t\tkeyLabel b keyLabel\t\t\t key\t\t\tkey \t\t\tkeyLabel n keyLabel\t\t\t\tpopupCharacters ňńñ popupCharacters\t\t\t\tpopupKeyboard xml/keyboard_popup_template popupKeyboard\t\t\t key\t\t\tkey \t\t\tkeyLabel m keyLabel\t\t\t key\t\t\tkey \t\t\tcode -5 code\t\t\t\tisRepeatable true isRepeatable\t\t\t\tkeyEdgeFlags right keyEdgeFlags\t\t\t\tkeyIcon ic_clear_vector keyIcon\t\t\t\tkeyWidth 15 keyWidth\t\t\t key\t\t row\t\trow \t\tkeyboardMode0 true keyboardMode0\t\t\tkeyboardMode1 true keyboardMode1\t\t\tkeyboardMode2 true keyboardMode2\t\t\tkey \t\t\tcode -2 code\t\t\t\tkeyEdgeFlags left keyEdgeFlags\t\t\t\tkeyLabel 123 keyLabel\t\t\t\tkeyWidth 15 keyWidth\t\t\t key\t\t\tkey \t\t\tcode -3 code\t\t\t\tkeyLabel \uD83C\uDF0E keyLabel\t\t\t\tkeyWidth 10 keyWidth\t\t\t key\t\t\tkey \t\t\tcode 32 code\t\t\t\tisRepeatable true isRepeatable\t\t\t\tkeyWidth 50 keyWidth\t\t\t key\t\t\tkey \t\t\tkeyLabel . keyLabel\t\t\t\tkeyWidth 10 keyWidth\t\t\t key\t\t\tkey \t\t\tcode -4 code\t\t\t\tkeyEdgeFlags right keyEdgeFlags\t\t\t\tkeyIcon ic_enter_vector keyIcon\t\t\t\tkeyWidth 15 keyWidth\t\t\t key\t\t row\t"

        fun toast(context: Context, text: String) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate. Some of the key size defaults can be overridden per row from
     * what the [MyKeyboard] defines.
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     */
    class Row {
        /** Default width of a key in this row.  */
        var defaultWidth = 0

        /** Default height of a key in this row.  */
        var defaultHeight = 0

        /** Default horizontal gap between keys in this row.  */
        var defaultHorizontalGap = 0

        var mKeys = ArrayList<Key>()

        var parent: MyKeyboard

        constructor(parent: MyKeyboard) {
            this.parent = parent
        }

        constructor(res: Resources, parent: MyKeyboard, data: String) {
            this.parent = parent
//            val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
//            defaultWidth = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, parent.mDisplayWidth, parent.mDefaultWidth)
            defaultWidth = getRMSLattr(data, "rowKeyWidth", null)?.toFloat()?.times(parent.mDisplayWidth)?.div(100)?.roundToInt() ?: parent.mDefaultWidth
            defaultHeight = (res.getDimension(R.dimen.key_height) * this.parent.mKeyboardHeightMultiplier).roundToInt()
//            defaultHorizontalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, parent.mDisplayWidth, parent.mDefaultHorizontalGap)
            val tempGap: Float = getRMSLattr(data, "rowHGap%", null)?.toFloat() ?: (100f*parent.mDefaultHorizontalGap*parent.mDisplayWidth)
            defaultHorizontalGap = (parent.mDisplayWidth * tempGap / 100).roundToInt()
            defaultHorizontalGap = getRMSLattr(data, "rowHGapPx", null)?.toFloat()?.roundToInt() ?: defaultHorizontalGap
//            a.recycle()
        }

        constructor(res: Resources, parent: MyKeyboard, parser: XmlResourceParser?) {
            this.parent = parent
            val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
            defaultWidth = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, parent.mDisplayWidth, parent.mDefaultWidth)
            defaultHeight = (res.getDimension(R.dimen.key_height) * this.parent.mKeyboardHeightMultiplier).roundToInt()
            defaultHorizontalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, parent.mDisplayWidth, parent.mDefaultHorizontalGap)
            a.recycle()
        }
    }

    /**
     * Class for describing the position and characteristics of a single key in the keyboard.
     *
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_keyHeight
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     * @attr ref android.R.styleable#Keyboard_Key_codes
     * @attr ref android.R.styleable#Keyboard_Key_keyIcon
     * @attr ref android.R.styleable#Keyboard_Key_keyLabel
     * @attr ref android.R.styleable#Keyboard_Key_isRepeatable
     * @attr ref android.R.styleable#Keyboard_Key_popupKeyboard
     * @attr ref android.R.styleable#Keyboard_Key_popupCharacters
     * @attr ref android.R.styleable#Keyboard_Key_keyEdgeFlags
     */
    class Key(parent: Row) {
        /** Key code that this key generates.  */
        var code = 0

        /** Label to display  */
        var label: CharSequence = ""

        /** First row of letters can also be used for inserting numbers by long pressing them, show those numbers  */
        var topSmallNumber: String = ""

        /** Icon to display instead of a label. Icon takes precedence over a label  */
        var icon: Drawable? = null

        /** Width of the key, not including the gap  */
        var width: Int

        /** Height of the key, not including the gap  */
        var height: Int

        /** The horizontal gap before this key  */
        var gap: Int

        /** X coordinate of the key in the keyboard layout  */
        var x = 0

        /** Y coordinate of the key in the keyboard layout  */
        var y = 0

        /** The current pressed state of this key  */
        var pressed = false

        /** Focused state, used after long pressing a key and swiping to alternative keys  */
        var focused = false

        /** Popup characters showing after long pressing the key  */
        var popupCharacters: CharSequence? = null

        /**
         * Flags that specify the anchoring to edges of the keyboard for detecting touch events that are just out of the boundary of the key.
         * This is a bit mask of [MyKeyboard.EDGE_LEFT], [MyKeyboard.EDGE_RIGHT].
         */
        private var edgeFlags = 0

        /** The keyboard that this key belongs to  */
        private val keyboard = parent.parent

        /** If this key pops up a mini keyboard, this is the resource id for the XML layout for that keyboard.  */
        var popupResId = 0

        /** Whether this key repeats itself when held down  */
        var repeatable = false

        /** Create a key with the given top-left coordinate and extract its attributes from the RMSL parser.
         * @param res resources associated with the caller's context
         * @param context the caller's context
         * @param parent the row that this key belongs to. The row must already be attached to a [MyKeyboard].
         * @param x the x coordinate of the top-left
         * @param y the y coordinate of the top-left
         * @param data the data containing the attributes for this key
         */
        @SuppressLint("UseCompatLoadingForDrawables")
        constructor(res: Resources, context: Context, parent: Row, x: Int, y: Int, data: String) : this(parent) {
            this.x = x
            this.y = y
//            var a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
//            width = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, keyboard.mDisplayWidth, parent.defaultWidth)
            width = getRMSLattr(data, "keyWidth", null)?.toFloat()?.times(keyboard.mDisplayWidth)?.div(100)?.roundToInt() ?: parent.defaultWidth
            height = parent.defaultHeight
//            gap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, keyboard.mDisplayWidth, parent.defaultHorizontalGap)
            val tempGap: Float = getRMSLattr(data, "hGap%", null)?.toFloat() ?: (100f*parent.defaultHorizontalGap/keyboard.mDisplayWidth)
            gap = (keyboard.mDisplayWidth * tempGap / 100).roundToInt()
            gap = getRMSLattr(data, "hGapPx", null)?.toFloat()?.roundToInt() ?: gap
            this.x += gap

//            a.recycle()
//            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard_Key)
//            code = a.getInt(R.styleable.MyKeyboard_Key_code, 0)
            code = getRMSLattr(data, "code", null)?.toInt() ?: 0

//            popupCharacters = a.getText(R.styleable.MyKeyboard_Key_popupCharacters)
            popupCharacters = getRMSLattr(data, "popupCharacters", null)
//            popupResId = a.getResourceId(R.styleable.MyKeyboard_Key_popupKeyboard, 0)
            popupResId = if (getRMSLattr(data, "popupKeyboard", null) == "xml/keyboard_popup_template") R.xml.keyboard_popup_template else 0
//            repeatable = a.getBoolean(R.styleable.MyKeyboard_Key_isRepeatable, false)
            repeatable = getRMSLattr(data, "isRepeatable", "false") == "true"
//            edgeFlags = a.getInt(R.styleable.MyKeyboard_Key_keyEdgeFlags, 0)
            edgeFlags = when (getRMSLattr(data, "keyEdgeFlags", "")) {
                "left"  -> EDGE_LEFT
                "right" -> EDGE_RIGHT
                else    -> 0
            }
//            icon = a.getDrawable(R.styleable.MyKeyboard_Key_keyIcon)
            val iconName = getRMSLattr(data, "keyIcon", "")
            icon = if (iconName == "") null else res.getDrawable(res.getIdentifier(iconName, "drawable", context.packageName))
            icon?.setBounds(0, 0, icon!!.intrinsicWidth, icon!!.intrinsicHeight)

//            label = a.getText(R.styleable.MyKeyboard_Key_keyLabel) ?: ""
            label = getRMSLattr(data, "keyLabel", null) ?: ""
//            topSmallNumber = a.getString(R.styleable.MyKeyboard_Key_topSmallNumber) ?: ""
            topSmallNumber = getRMSLattr(data, "topSmallNumber", null) ?: ""

            if (label.isNotEmpty() && code != KEYCODE_MODE_CHANGE && code != KEYCODE_SHIFT) {
                code = label[0].code
            }
//            a.recycle()
        }

        /** Create a key with the given top-left coordinate and extract its attributes from the XML parser.
         * @param res resources associated with the caller's context
         * @param parent the row that this key belongs to. The row must already be attached to a [MyKeyboard].
         * @param x the x coordinate of the top-left
         * @param y the y coordinate of the top-left
         * @param parser the XML parser containing the attributes for this key
         */
        constructor(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser?) : this(parent) {
            this.x = x
            this.y = y
            var a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
            width = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, keyboard.mDisplayWidth, parent.defaultWidth)
            height = parent.defaultHeight
            gap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, keyboard.mDisplayWidth, parent.defaultHorizontalGap)
            this.x += gap

            a.recycle()
            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard_Key)
            code = a.getInt(R.styleable.MyKeyboard_Key_code, 0)

            popupCharacters = a.getText(R.styleable.MyKeyboard_Key_popupCharacters)
            popupResId = a.getResourceId(R.styleable.MyKeyboard_Key_popupKeyboard, 0)
            repeatable = a.getBoolean(R.styleable.MyKeyboard_Key_isRepeatable, false)
            edgeFlags = a.getInt(R.styleable.MyKeyboard_Key_keyEdgeFlags, 0)
            icon = a.getDrawable(R.styleable.MyKeyboard_Key_keyIcon)
            icon?.setBounds(0, 0, icon!!.intrinsicWidth, icon!!.intrinsicHeight)

            label = a.getText(R.styleable.MyKeyboard_Key_keyLabel) ?: ""
            topSmallNumber = a.getString(R.styleable.MyKeyboard_Key_topSmallNumber) ?: ""

            if (label.isNotEmpty() && code != KEYCODE_MODE_CHANGE && code != KEYCODE_SHIFT) {
                code = label[0].code
            }
            a.recycle()
        }

        /** Create an empty key with no attributes.  */
        init {
            height = parent.defaultHeight
            width = parent.defaultWidth
            gap = parent.defaultHorizontalGap
        }

        /**
         * Detects if a point falls inside this key.
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return whether or not the point falls inside the key. If the key is attached to an edge, it will assume that all points between the key and
         * the edge are considered to be inside the key.
         */
        fun isInside(x: Int, y: Int): Boolean {
            val leftEdge = edgeFlags and EDGE_LEFT > 0
            val rightEdge = edgeFlags and EDGE_RIGHT > 0
            return ((x >= this.x || leftEdge && x <= this.x + width)
                && (x < this.x + width || rightEdge && x >= this.x)
                && (y >= this.y && y <= this.y + height)
                && (y < this.y + height && y >= this.y))
        }
    }

    /**
     * Creates a keyboard from the given rmsl key layout file. Weeds out rows that have a keyboard mode defined but don't match the specified mode.
     * @param context the application or service context
     * @param enterKeyType determines what icon should we show on Enter key
     * @param keyboardMode tells loader which tag the rows should have
     * @param _1 this is only here to distinguish the constructor from the second one
     */
    constructor(context: Context, enterKeyType: Int, keyboardMode: Int, _1: Int) {
        mDisplayWidth = context.resources.displayMetrics.widthPixels
        mDefaultHorizontalGap = 0
        mDefaultWidth = mDisplayWidth / 10
        mDefaultHeight = mDefaultWidth
        mKeyboardHeightMultiplier = getKeyboardHeightMultiplier(context.config.keyboardHeightMultiplier)
        mKeys = ArrayList()
        mEnterKeyType = enterKeyType

//        loadKeyboard(context, context.resources.getXml(xmlLayoutResId))
        loadKeyboard(context, readKeyboardFile(context), keyboardMode)
    }

    private fun readKeyboardFile(context: Context): String {
        val fileName = "${context.config.keyboardLanguage}.rmsl"
        val file = File(Environment.DIRECTORY_DOCUMENTS, fileName)
        if (!file.exists()) {
            if (context.config.keyboardLanguage != 0) {
                context.config.keyboardLanguage = 0
                return readKeyboardFile(context)
            }
            return DEFAULT_KEYBOARD
        }

        val fileIS: InputStream? = createInput(file)
        if (fileIS != null) {
            val fileBR = BufferedReader(InputStreamReader(fileIS, StandardCharsets.UTF_8))
            val out: String = loadString(fileBR)
            try { fileIS.close() }
            catch (e: IOException) { e.printStackTrace() }
            return out
        }
        return DEFAULT_KEYBOARD
    }

    private fun createInput(file: File?): InputStream? {
        requireNotNull(file) { "File passed to createInput() was null" }
        return try {
            val input: InputStream = FileInputStream(file)
            val lower = file.name.lowercase(Locale.getDefault())
            if (lower.endsWith(".gz") || lower.endsWith(".svgz")) {
                BufferedInputStream(GZIPInputStream(input))
            } else BufferedInputStream(input)
        } catch (e: IOException) {
            System.err.println("Could not createInput() for $file")
            e.printStackTrace()
            null
        }
    }

    private fun loadString(reader: BufferedReader): String {
        try {
            var lines = arrayOfNulls<String>(100)
            var lineCount = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (lineCount == lines.size) {
                    val temp = arrayOfNulls<String>(lineCount shl 1)
                    System.arraycopy(lines, 0, temp, 0, lineCount)
                    lines = temp
                }
                lines[lineCount++] = line
            }
            reader.close()
            if (lineCount == lines.size) {
                var out = ""
                lines.forEach {
                    out += it
                }
                return out
            }

            // resize array to appropriate amount for these lines
            val output = arrayOfNulls<String>(lineCount)
            System.arraycopy(lines, 0, output, 0, lineCount)
            var out = ""
            output.forEach {
                out += it
            }
            return out
        } catch (e: IOException) {
            e.printStackTrace()
            //throw new RuntimeException("Error inside loadStrings()");
        }
        return DEFAULT_KEYBOARD
    }

//    private fun readDoc(context: Context, fileName: String): String {
//        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
////        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_INTERNAL)
////        val collection = MediaStore.Files.getContentUri(Environment.getExternalStorageDirectory().toString())
//        val dirDest = File(Environment.DIRECTORY_DOCUMENTS, context.getString(R.string.app_name))
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
//            put(MediaStore.MediaColumns.RELATIVE_PATH, "$dirDest${File.separator}")
//            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
//        }
//        val docUri = context.contentResolver.insert(collection, contentValues)
//
//        val resolver: ContentResolver = context.contentResolver
//        resolver.openInputStream(docUri!!)?.use {
//                stream -> return@readDoc stream.readBytes().toString(Charsets.UTF_8)
//        }
//            ?: throw IllegalStateException("could not open $docUri")
//    }

//    private suspend fun readDoc(context: Context, fileName: String): String = withContext(Dispatchers.IO) {
//        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//        val dirDest = File(Environment.DIRECTORY_DOCUMENTS, context.getString(R.string.app_name))
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
//            put(MediaStore.MediaColumns.RELATIVE_PATH, "$dirDest${File.separator}")
//            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
//        }
//        val docUri = context.contentResolver.insert(collection, contentValues)
//
//        val resolver: ContentResolver = context.contentResolver
//        resolver.openInputStream(docUri!!)?.use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
//            ?: throw IllegalStateException("could not open $docUri")
//    }
//
    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows that have a keyboard mode defined but don't match the specified mode.
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param enterKeyType determines what icon should we show on Enter key
     */
    constructor(context: Context, @XmlRes xmlLayoutResId: Int, enterKeyType: Int) {
        mDisplayWidth = context.resources.displayMetrics.widthPixels
        mDefaultHorizontalGap = 0
        mDefaultWidth = mDisplayWidth / 10
        mDefaultHeight = mDefaultWidth
        mKeyboardHeightMultiplier = getKeyboardHeightMultiplier(context.config.keyboardHeightMultiplier)
        mKeys = ArrayList()
        mEnterKeyType = enterKeyType
        loadKeyboard(context, context.resources.getXml(xmlLayoutResId))
    }

    /**
     * Creates a blank keyboard from the given resource file and populates it with the specified characters in left-to-right, top-to-bottom fashion,
     * using the specified number of columns. If the specified number of columns is -1, then the keyboard will fit as many keys as possible in each row.
     * @param context the application or service context
     * @param layoutTemplateResId the layout template file, containing no keys.
     * @param characters the list of characters to display on the keyboard. One key will be created for each character.
     * @param keyWidth the width of the popup key, make sure it is the same as the key itself
     */
    constructor(context: Context, layoutTemplateResId: Int, characters: CharSequence, keyWidth: Int) :
        this(context, layoutTemplateResId, 0) {
        var x = 0
        var y = 0
        var column = 0
        mMinWidth = 0
        val row = Row(this)
        row.defaultHeight = mDefaultHeight
        row.defaultWidth = keyWidth
        row.defaultHorizontalGap = mDefaultHorizontalGap
        mKeyboardHeightMultiplier = getKeyboardHeightMultiplier(context.config.keyboardHeightMultiplier)

        characters.forEachIndexed { index, character ->
            val key = Key(row)
            if (column >= MAX_KEYS_PER_MINI_ROW) {
                column = 0
                x = 0
                y += mDefaultHeight
                mRows.add(row)
                row.mKeys.clear()
            }

            key.x = x
            key.y = y
            key.label = character.toString()
            key.code = character.code
            column++
            x += key.width + key.gap
            mKeys!!.add(key)
            row.mKeys.add(key)
            if (x > mMinWidth) {
                mMinWidth = x
            }
        }
        mHeight = y + mDefaultHeight
        mRows.add(row)
    }

    fun setShifted(shiftState: Int): Boolean {
        if (this.mShiftState != shiftState) {
            this.mShiftState = shiftState
            return true
        }

        return false
    }

    private fun createRowFromRmsl(res: Resources, data: String): Row {
        return Row(res, this, data)
    }

    private fun createRowFromXml(res: Resources, parser: XmlResourceParser?): Row {
        return Row(res, this, parser)
    }

    private fun createKeyFromRmsl(res: Resources, context: Context, parent: Row, x: Int, y: Int, data: String): Key {
        return Key(res, context, parent, x, y, data)
    }

    private fun createKeyFromXml(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser?): Key {
        return Key(res, parent, x, y, parser)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadKeyboard(context: Context, data: String, keyboardMode: Int) {
        var row = 0
        var x: Int
        var y = 0
        var key: Key?
        var currentRow: Row?
        val res = context.resources
        try {
            parseKeyboardAttributes(res, data)

            val rowStrs: ArrayList<String> = getRMSLattrs(data, "row")
            rowStrs.forEach { rowStr ->
                getRMSLattr(rowStr, "keyboardMode$keyboardMode", null) ?: return@forEach
                x = 0
                currentRow = createRowFromRmsl(res, rowStr)
                mRows.add(currentRow)

                val keyStrs: ArrayList<String> = getRMSLattrs(rowStr, "key")
                keyStrs.forEach { keyStr ->
                    key = createKeyFromRmsl(res, context, currentRow!!, x, y, keyStr)
                    mKeys!!.add(key)
                    if (key!!.code == KEYCODE_ENTER) {
                        val enterResourceId = when (mEnterKeyType) {
                            EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_search_vector
                            EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_GO -> R.drawable.ic_arrow_right_vector
                            EditorInfo.IME_ACTION_SEND -> R.drawable.ic_send_vector
                            else -> R.drawable.ic_enter_vector
                        }
                        key!!.icon = context.resources.getDrawable(enterResourceId, context.theme)
                    }
                    currentRow!!.mKeys.add(key!!)
                    x += key!!.gap + key!!.width
                    if (x > mMinWidth) {
                        mMinWidth = x
                    }
                }

                y += currentRow!!.defaultHeight
                row++
            }
        } catch (_: Exception) {
        }

        mHeight = y
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadKeyboard(context: Context, parser: XmlResourceParser) {
        var inKey = false
        var inRow = false
        var row = 0
        var x = 0
        var y = 0
        var key: Key? = null
        var currentRow: Row? = null
        val res = context.resources
        try {
            var event: Int
            while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    when (parser.name) {
                        TAG_ROW -> {
                            inRow = true
                            x = 0
                            currentRow = createRowFromXml(res, parser)
                            mRows.add(currentRow)
                        }
                        TAG_KEY -> {
                            inKey = true
                            key = createKeyFromXml(res, currentRow!!, x, y, parser)
                            mKeys!!.add(key)
                            if (key.code == KEYCODE_ENTER) {
                                val enterResourceId = when (mEnterKeyType) {
                                    EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_search_vector
                                    EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_GO -> R.drawable.ic_arrow_right_vector
                                    EditorInfo.IME_ACTION_SEND -> R.drawable.ic_send_vector
                                    else -> R.drawable.ic_enter_vector
                                }
                                key.icon = context.resources.getDrawable(enterResourceId, context.theme)
                            }
                            currentRow.mKeys.add(key)
                        }
                        TAG_KEYBOARD -> {
                            parseKeyboardAttributes(res, parser)
                        }
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false
                        x += key!!.gap + key.width
                        if (x > mMinWidth) {
                            mMinWidth = x
                        }
                    } else if (inRow) {
                        inRow = false
                        y += currentRow!!.defaultHeight
                        row++
                    }
                }
            }
        } catch (_: Exception) {
        }

        mHeight = y
    }

    private fun parseKeyboardAttributes(res: Resources, data: String) {
//        val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
//        mDefaultWidth = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, mDisplayWidth, mDisplayWidth / 10)
        val tempWidth: Float = getRMSLattr(data, "keyboardKeyWidth", null)?.toFloat() ?: (100f/10)
        mDefaultWidth = (mDisplayWidth * tempWidth / 100).roundToInt()
//        mDefaultHeight = res.getDimension(R.dimen.key_height).toInt()
        mDefaultHeight = getRMSLattr(data, "keyboardRowHeight", null)?.toFloat()?.roundToInt() ?: res.getDimension(R.dimen.key_height).toInt()
//        mDefaultHorizontalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, mDisplayWidth, 0)
        val tempGap: Float = getRMSLattr(data, "keyboardHGap%", null)?.toFloat() ?: 0f
        mDefaultHorizontalGap = (mDisplayWidth * tempGap / 100).roundToInt()
        mDefaultHorizontalGap = getRMSLattr(data, "keyboardHGapPx", null)?.toFloat()?.roundToInt() ?: mDefaultHorizontalGap
//        a.recycle()
    }

    private fun parseKeyboardAttributes(res: Resources, parser: XmlResourceParser) {
        val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.MyKeyboard)
        mDefaultWidth = getDimensionOrFraction(a, R.styleable.MyKeyboard_keyWidth, mDisplayWidth, mDisplayWidth / 10)
        mDefaultHeight = res.getDimension(R.dimen.key_height).toInt()
        mDefaultHorizontalGap = getDimensionOrFraction(a, R.styleable.MyKeyboard_horizontalGap, mDisplayWidth, 0)
        a.recycle()
    }

    private fun getKeyboardHeightMultiplier(multiplierType: Int): Float {
        return when(multiplierType) {
            KEYBOARD_HEIGHT_MULTIPLIER_SMALL -> 1.0F
            KEYBOARD_HEIGHT_MULTIPLIER_MEDIUM -> 1.2F
            KEYBOARD_HEIGHT_MULTIPLIER_LARGE -> 1.4F
            else -> 1.0F
        }
    }
}
