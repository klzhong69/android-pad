package com.cqebd.student.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.cqebd.student.repository.SubscribeRepository
import com.cqebd.student.vo.Resource
import com.cqebd.student.vo.entity.CourseInfo

/**
 * 订阅视频
 * Created by xiaofu on 2018/3/21.
 */
class SubscribeListViewModel : ViewModel(){
    private val repository = SubscribeRepository()

    var subscribeList = MutableLiveData<List<CourseInfo>>()

    fun getSubscribeList():LiveData<Resource<List<CourseInfo>>> {
        return repository.getSubscribeList()
    }

    fun setData(subscribeList:List<CourseInfo>){
        this.subscribeList.value = subscribeList
    }

}