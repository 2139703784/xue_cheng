package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.common.State;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CourseMarketService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/8 9:46
 */
@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    CourseMarketService courseMarketService;


    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams params, QueryCourseParamsDto queryCourseParamsDto) {

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        //拼接查询条件
        //根据课程名称模糊查询  name like '%名称%'
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());

        //根据课程审核状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());

        //根据课程发布状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());


        //分页参数
        Page<CourseBase> page = new Page<>(params.getPageNo(), params.getPageSize());


        //分页查询E page 分页参数, @Param("ew") Wrapper<T> queryWrapper 查询条件
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        //数据
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();


        //准备返回数据 List<T> items, long counts, long page, long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(items, total, params.getPageNo(), params.getPageSize());

        return courseBasePageResult;
    }



    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
//        //对参数进行合法性的校验
//        //合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
//            XuechengPlusException.cast("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            XuechengPlusException.cast("课程分类为空");
//
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            XuechengPlusException.cast("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            XuechengPlusException.cast("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            XuechengPlusException.cast("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            XuechengPlusException.cast("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            XuechengPlusException.cast("收费规则为空");
//        }
         //对数据进行封装，调用mapper进行数据持久化
        CourseBase courseBase = new CourseBase();
        //将传入dto的数据设置到 courseBase对象
//        courseBase.setName(dto.getName());
//        courseBase.setMt(dto.getMt());
//        courseBase.setSt(dto.getSt());
        //将dto中和courseBase属性名一样的属性值拷贝到courseBase
        BeanUtils.copyProperties(dto,courseBase);
        //设置机构id
        courseBase.setCompanyId(companyId);
        //创建时间
        courseBase.setCreateDate(LocalDateTime.now());
        //审核状态默认为未提交
        courseBase.setAuditStatus(State.STATE_UNCOMMITTED);
        //发布状态默认为未发布
        courseBase.setStatus(State.STATE_UNPUBLISHED);
        //课程基本表插入一条记录
        int insert = courseBaseMapper.insert(courseBase);
        //获取课程id
        Long courseId = courseBase.getId();
        CourseMarket courseMarket = new CourseMarket();
        //将dto中和courseMarket属性名一样的属性值拷贝到courseMarket
        BeanUtils.copyProperties(dto,courseMarket);
        courseMarket.setId(courseId);
        int insert1 = this.saveCourseMarket(courseMarket);

        if(insert<=0|| insert1<=0){
            throw new RuntimeException("添加课程失败");
        }

        //组装要返回的结果
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto) {
        //校验
        //课程id
        Long id = dto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if(courseBase==null){
           XuechengPlusException.cast("课程不存在");
        }

        //校验本机构只能修改本机构的课程
        if(!courseBase.getCompanyId().equals(companyId)){
            XuechengPlusException.cast("本机构只能修改本机构的课程");
        }

        //封装基本信息的数据
        BeanUtils.copyProperties(dto,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());

        //更新课程基本信息
        int i = courseBaseMapper.updateById(courseBase);

        //封装营销信息的数据
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);

        this.saveCourseMarket(courseMarket);

        CourseBaseInfoDto courseBaseInfo = this.getCourseBaseInfo(id);
        return  courseBaseInfo;
    }

        //抽取对营销的保存
        private int saveCourseMarket(CourseMarket courseMarket) {
            // 校验如果课程为收费，必须输入价格且大于0
            String charge = courseMarket.getCharge();
            if (StringUtils.isBlank(charge)) {
                XuechengPlusException.cast("收费规则没有选则");
            }
            if (charge.equals("201001")) {
                if (courseMarket.getPrice() == null || courseMarket.getPrice().floatValue() <= 0) {
//              throw new RuntimeException("课程为收费价格不能为空且必须大于0");
                    XuechengPlusException.cast("课程为收费价格不能为空且必须大于0");

                }
            }
            boolean b = courseMarketService.saveOrUpdate(courseMarket);
            return b?1:0;
        }

    /**
     * 根据课程id查询课程的基本和营销信息
     * @param courseId 课程id
     * @return 课程的信息
     */
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){

        //基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);

        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);


        if (courseMarket!=null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        //根据课程分类的id查询分类的名称
        String mt = courseBase.getMt();
        String st = courseBase.getSt();


        CourseCategory mtCategory = courseCategoryMapper.selectById(mt);
        CourseCategory stCategory = courseCategoryMapper.selectById(st);
        if(mtCategory!=null){
            //分类名称
            String mtName = mtCategory.getName();
            courseBaseInfoDto.setMtName(mtName);
        }
        if(stCategory!=null){
            //分类名称
            String stName = stCategory.getName();
            courseBaseInfoDto.setStName(stName);
        }


        return courseBaseInfoDto;

    }


}