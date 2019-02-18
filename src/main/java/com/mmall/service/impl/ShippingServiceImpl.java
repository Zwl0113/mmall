package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mmall.common.ServerResponse;
import com.mmall.dao.ShippingMapper;
import com.mmall.dto.Shipping;
import com.mmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {
    @Autowired
    private ShippingMapper shippingMapper;

    public ServerResponse add(Integer userId, Shipping shipping){
        shipping.setUserId(userId);
        int rowCount = 0;
        try{
            rowCount = shippingMapper.insert(shipping);
        }catch (Exception e){
            e.printStackTrace();
        }

        if(rowCount > 0){
            Map resultMap = Maps.newHashMap();
            resultMap.put("shippingId",shipping.getId());
            return ServerResponse.createBySuccess("新建地址成功",resultMap);
        }
        return ServerResponse.createBySuccessMessage("新建地址失败");
    }

    public ServerResponse<String> del(Integer userId,Integer shippingId){
        int resultCount = 0;
        try{
            resultCount = shippingMapper.deleteByShippingIdUserId(userId, shippingId);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(resultCount > 0){
            return ServerResponse.createBySuccessMessage("删除地址成功");
        }
        return ServerResponse.createByErrorMessage("删除地址失败");
    }

    public ServerResponse update(Integer userId, Shipping shipping){
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByShipping(shipping);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("更新地址成功");
        }
        return ServerResponse.createBySuccessMessage("更新地址失败");
    }

    public ServerResponse<Shipping> select(Integer userId,Integer shippingId){
        Shipping shipping = null;
        try{
            shipping = shippingMapper.selectByShippingIdUserId(userId, shippingId);
        }catch (Exception e){
            e.printStackTrace();
        }

        if(shipping != null){
            return ServerResponse.createBySuccess("查询地址成功",shipping);
        }
        return ServerResponse.createByErrorMessage("查询地址失败");
    }

    public ServerResponse<PageInfo> list(Integer userId,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> shippings = shippingMapper.selectByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippings);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
