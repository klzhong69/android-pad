package com.cqebd.student.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import com.anko.static.dp
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cqebd.student.R
import com.cqebd.student.app.App
import com.cqebd.student.app.BaseActivity
import com.cqebd.student.db.entity.ClassSchedule
import com.cqebd.student.glide.GlideApp
import com.cqebd.student.tools.colorForRes
import com.cqebd.student.tools.formatTime
import com.cqebd.student.tools.formatTimeYMDHM
import com.cqebd.student.viewmodel.ClassScheduleViewModel
import com.cqebd.student.vo.Resource
import com.cqebd.student.vo.entity.CourseInfo
import com.cqebd.teacher.vo.Status
import com.prolificinteractive.materialcalendarview.*
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import gorden.lib.anko.static.logError
import kotlinx.android.synthetic.main.activity_class_schedule.*
import kotlinx.android.synthetic.main.item_course.view.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * 课程表
 * Created by gorden on 2018/3/12.
 */
class ClassScheduleActivity : BaseActivity(), OnMonthChangedListener, OnDateSelectedListener, Observer<Resource<ClassSchedule>> {
    private lateinit var currentDate: Calendar
    private lateinit var selectedDate: Calendar
    private lateinit var viewModel: ClassScheduleViewModel
    private val courseList: ArrayList<CourseInfo> = ArrayList()
    private lateinit var adapter: BaseQuickAdapter<CourseInfo, BaseViewHolder>
    override fun setContentView() {
        setContentView(R.layout.activity_class_schedule)
    }


    override fun initialize(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(ClassScheduleViewModel::class.java)
        adapter = object : BaseQuickAdapter<CourseInfo, BaseViewHolder>(R.layout.item_course) {
            override fun convert(helper: BaseViewHolder?, item: CourseInfo) {
                helper?.itemView?.apply {
                    text_name.text = item.Name
                    text_grade.text = "年级:".plus(item.GradeName)
                    text_start.text = "开课时间:".plus(formatTimeYMDHM(item.PlanStartDate))
                    text_teacher.text = "主讲老师:".plus(item.TeacherName)
                    GlideApp.with(App.mContext).asBitmap().load(item.Snapshoot).into(img_snapshoot)

                    when(item.Status){
                        0->{
                            text_label.text = "未开始"
                            text_label.setColors(Color.parseColor("#66000000"),Color.TRANSPARENT,Color.TRANSPARENT)
                        }
                        1->{
                            text_label.text = "直播中"
                            text_label.setColors(colorForRes(R.color.colorPrimary),Color.TRANSPARENT,Color.TRANSPARENT)
                        }
                        2->{
                            text_label.text = "直播结束"
                            text_label.setColors(Color.parseColor("#66000000"),Color.TRANSPARENT,Color.TRANSPARENT)
                        }
                        3->{
                            text_label.text = "回放"
                            text_label.setColors(colorForRes(R.color.colorPrimary),Color.TRANSPARENT,Color.TRANSPARENT)
                        }
                    }

                }
            }
        }
        recyclerView.adapter = adapter
        //设置可查看前一年和后一年的课程表
        val min = Calendar.getInstance()
        min.set(min.get(Calendar.YEAR) - 1, Calendar.JANUARY, 1)
        val max = Calendar.getInstance()
        max.set(max.get(Calendar.YEAR) + 1, Calendar.DECEMBER, 31)
        calendarView.state().edit().setMinimumDate(min).setMaximumDate(max).commit()

        currentDate = Calendar.getInstance()
        currentDate.set(Calendar.HOUR_OF_DAY, 0)
        currentDate.set(Calendar.MINUTE, 0)
        currentDate.set(Calendar.SECOND, 0)
        currentDate.set(Calendar.MILLISECOND, 0)
        selectedDate = currentDate
        calendarView.selectedDate = CalendarDay.from(selectedDate)
        viewModel.getPeriodListMonth(selectedDate).observe(this, this)
    }

    override fun bindEvents() {
        calendarView.setOnMonthChangedListener(this)
        calendarView.setOnDateChangedListener(this)
    }

    override fun onMonthChanged(widget: MaterialCalendarView?, date: CalendarDay?) {
        date?.calendar?.apply {
            this.set(Calendar.DAY_OF_MONTH, if (selectedDate.get(Calendar.DAY_OF_MONTH) > getActualMaximum(Calendar.DAY_OF_MONTH)) 1 else selectedDate.get(Calendar.DAY_OF_MONTH))
            selectedDate = this
            calendarView.selectedDate = CalendarDay.from(selectedDate)
            viewModel.getPeriodListMonth(selectedDate).observe(this@ClassScheduleActivity, this@ClassScheduleActivity)
        }
    }

    override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
        selectedDate = date.calendar
        filterData()
    }

    override fun onChanged(it: Resource<ClassSchedule>?) {
        when (it?.status) {
            Status.SUCCESS -> {
                pageLoadView.hide()
                logError("count ${it.data?.courses?.size}  " + it.data?.courses)
                calendarView.removeDecorators()
                val dates = it.data?.courses?.map {
                    CalendarDay.from(Date(formatTime(it.PlanStartDate)))
                }
                if (dates != null && dates.isNotEmpty()) {
                    calendarView.addDecorator(LiveDecorator(dates))
                }

                courseList.clear()
                it.data?.courses?.apply {
                    courseList.addAll(this)
                }
                filterData()
            }
            Status.ERROR -> {
                pageLoadView.error({
                    viewModel.getPeriodListMonth(selectedDate).observe(this, this)
                })
            }
            Status.LOADING -> {
                pageLoadView.show = true
                pageLoadView.load()
            }
        }
    }

    private fun filterData() {
        courseList.filter {
            CalendarDay.from(Date(formatTime(it.PlanStartDate))).calendar == selectedDate
        }.apply {
            if (isEmpty()){
                pageLoadView.show = true
                pageLoadView.dataEmpty()
            }else{
                pageLoadView.hide()
            }
            adapter.setNewData(this)
        }
    }

    /**
     * CalendarView存在直播点播的装饰
     */
    class LiveDecorator(private val dates: List<CalendarDay>) : DayViewDecorator {
        private var dotColor: Int = Color.RED
        override fun shouldDecorate(day: CalendarDay?): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade?) {
            view?.addSpan(DotSpan(3.dp.toFloat(), dotColor))
        }
    }

}