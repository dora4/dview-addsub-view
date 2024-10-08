package dora.widget

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import dora.widget.addsubview.R

class AddSubView(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs), View.OnClickListener, TextWatcher {

    /**
     * 最大购买数量，默认为99。
     */
    private var max = 99

    /**
     * 购买数量。
     */
    private var inputValue = 1

    /**
     * 商品库存，默认最大值。
     */
    private var inventory = 99

    /**
     * 商品最小购买数量，默认值为0。
     */
    private var min = 0

    /**
     * 步长--每次增加的个数，默认是1。
     */
    private var step = 1

    /**
     * 设置改变的位置，默认是0。
     */
    private var position = 0
    private var onWarnListener: OnWarnListener? = null
    private var onChangeValueListener: OnChangeValueListener? = null
    private lateinit var etInput: EditText
    private lateinit var icPlus: ImageView
    private lateinit var icMinus: ImageView

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.AddSubView)
        val editable = a.getBoolean(R.styleable.AddSubView_dview_asv_editable, true)
        // 左右两面的宽度
        val imageWidth = a.getDimensionPixelSize(R.styleable.AddSubView_dview_asv_imageWidth, -1)
        // 中间内容框的宽度
        val contentWidth =
            a.getDimensionPixelSize(R.styleable.AddSubView_dview_asv_contentWidth, -1)
        // 中间字体的大小
        val contentTextSize =
            a.getDimensionPixelSize(R.styleable.AddSubView_dview_asv_contentTextSize, -1)
        // 中间字体的颜色
        val contentTextColor =
            a.getColor(R.styleable.AddSubView_dview_asv_contentTextColor, -0x1000000)
        // 整个控件的background
        val background = a.getDrawable(R.styleable.AddSubView_dview_asv_background)
        // 左面控件的背景
        val leftBackground = a.getDrawable(R.styleable.AddSubView_dview_asv_leftBackground)
        // 右面控件的背景
        val rightBackground = a.getDrawable(R.styleable.AddSubView_dview_asv_rightBackground)
        // 中间控件的背景
        val contentBackground = a.getDrawable(R.styleable.AddSubView_dview_asv_contentBackground)
        // 左面控件的资源
        val leftResources = a.getDrawable(R.styleable.AddSubView_dview_asv_leftResources)
        // 右面控件的资源
        val rightResources = a.getDrawable(R.styleable.AddSubView_dview_asv_rightResources)
        // 资源回收
        a.recycle()

        // 把布局和当前类形成整体
        LayoutInflater.from(context).inflate(R.layout.layout_add_sub, this)
        icPlus = findViewById<View>(R.id.ic_plus) as ImageView
        icMinus = findViewById<View>(R.id.ic_minus) as ImageView
        etInput = findViewById<View>(R.id.et_input) as EditText
        icPlus.setOnClickListener(this)
        icMinus.setOnClickListener(this)
        etInput.setOnClickListener(this)
        etInput.addTextChangedListener(this)
        setEditable(editable)
        etInput.setTextColor(contentTextColor)

        // 设置两边按钮的宽度
        if (imageWidth > 0) {
            val textParams = LayoutParams(imageWidth, LayoutParams.MATCH_PARENT)
            icPlus.layoutParams = textParams
            icMinus.layoutParams = textParams
        }

        // 设置中间输入框的宽度
        if (contentWidth > 0) {
            val textParams = LayoutParams(contentWidth, LayoutParams.MATCH_PARENT)
            etInput.layoutParams = textParams
        }
        if (contentTextColor > 0) {
            etInput.textSize = contentTextColor.toFloat()
        }
        if (contentTextSize > 0) {
            etInput.textSize = contentTextSize.toFloat()
        }
        if (contentBackground != null) {
            etInput.background = contentBackground
        }
        if (leftBackground != null) {
            icMinus.background = leftBackground
        }
        if (rightBackground != null) {
            icPlus.background = rightBackground
        }
        if (leftResources != null) {
            icMinus.setImageDrawable(leftResources)
        }
        if (rightResources != null) {
            icPlus.setImageDrawable(rightResources)
        }
    }

    private fun setEditable(editable: Boolean) {
        if (editable) {
            etInput.isFocusable = true
            etInput.keyListener = DigitsKeyListener()
        } else {
            etInput.isFocusable = false
            etInput.keyListener = null
        }
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.ic_plus) {
            // 加
            if (inputValue < max.coerceAtMost(inventory)) {
                inputValue += step
                // 正常添加
                if (etInput.isCursorVisible) {
                    etInput.isCursorVisible = false
                }
                etInput.setText(inputValue)
            } else if (inventory < max) {
                // 库存不足
                warningForInventory()
            } else {
                // 超过最大购买数
                warningForMax()
            }
        } else if (id == R.id.ic_minus) {
            // 减
            if (inputValue > min) {
                inputValue -= step
                if (etInput.isCursorVisible) {
                    etInput.isCursorVisible = false
                }
                etInput.setText(inputValue.toString())
            } else {
                // 低于最小购买数
                warningForMin()
            }
        } else if (id == R.id.et_input) {
            // 输入框
            if (!etInput.isCursorVisible) {
                etInput.isCursorVisible = true
            }
            etInput.setSelection(etInput.text.toString().length)
        }
    }

    /**
     * 低于最小购买数。
     * Warning for buy min.
     */
    private fun warningForMin() {
        icMinus.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context, R.color.color_icon
            )
        )
        onWarnListener?.onWarningForMin(min)
    }

    /**
     * 超过的最大购买数限制。
     * Warning for buy max.
     */
    private fun warningForMax() {
        icPlus.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context, R.color.color_icon
            )
        )
        onWarnListener?.onWarningForMax(max)
    }

    /**
     * 超过的库存限制。
     * Warning for inventory.
     */
    private fun warningForInventory() {
        onWarnListener?.onWarningForInventory(inventory)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        onNumberInput()
    }

    /**
     * 监听输入的数据变化。
     */
    private fun onNumberInput() {
        // 当前数量
        val count = getNumber()
        if (count < min) {
            // 手动输入
            inputValue = min
            etInput.setText(inputValue.toString())
            etInput.setSelection(etInput.text.toString().length)
            onChangeValueListener?.onChangeValue(inputValue, position)
            return
        }
        val limit = max.coerceAtMost(inventory)
        if (count > limit) {
            if (inventory < max) {
                // 库存不足
                warningForInventory()
            } else {
                // 超过最大购买数
                warningForMax()
            }
        } else if (count == min) {
            warningForMin()
        } else if (count == max) {
            warningForMax()
        } else {
            inputValue = count
            onChangeValueListener?.onChangeValue(inputValue, position)
            icPlus.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.color_text_bg
                )
            )
            icMinus.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.color_text_bg
                )
            )
        }
    }

    fun setCurrentNumber(currentNumber: Int): AddSubView {
        inputValue = if (currentNumber < min) {
            min
        } else {
            max.coerceAtMost(inventory).coerceAtMost(currentNumber)
        }
        etInput.setText(inputValue.toString())
        etInput.setSelection(etInput.text.toString().length)
        return this
    }

    fun getInventory(): Int {
        return inventory
    }

    fun setInventory(inventory: Int): AddSubView {
        this.inventory = inventory
        return this
    }

    fun setBean(bean: Any?): AddSubView {
        return this
    }

    fun getMax(): Int {
        return max
    }

    fun setMax(max: Int): AddSubView {
        this.max = max
        return this
    }

    fun setPosition(position: Int): AddSubView {
        this.position = position
        return this
    }

    fun getPosition(): Int {
        return position
    }

    fun setMin(min: Int): AddSubView {
        this.min = min
        return this
    }

    fun setOnWarnListener(listener: OnWarnListener): AddSubView {
        this.onWarnListener = listener
        return this
    }

    fun setOnChangeValueListener(listener: OnChangeValueListener): AddSubView {
        this.onChangeValueListener = listener
        return this
    }

    fun getStep(): Int {
        return step
    }

    fun setStep(step: Int): AddSubView {
        this.step = step
        return this
    }

    override fun afterTextChanged(s: Editable) {}

    /**
     * 得到输入框的数量。
     *
     * @return
     */
    fun getNumber(): Int {
        try {
            return if (TextUtils.isEmpty(etInput.text.toString())) min else etInput.text.toString()
                .toInt()
        } catch (ignore: NumberFormatException) {
        }
        return min
    }

    interface OnWarnListener {

        fun onWarningForInventory(inventory: Int)
        fun onWarningForMax(max: Int)
        fun onWarningForMin(min: Int)
    }

    interface OnChangeValueListener {
        fun onChangeValue(value: Int, position: Int)
    }
}