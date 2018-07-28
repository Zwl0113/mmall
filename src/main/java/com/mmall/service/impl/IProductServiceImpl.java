package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import com.mmall.service.IProductService;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.ProductDetailVO;
import com.mmall.vo.ProductListVO;
import net.sf.jsqlparser.schema.Server;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("iProductService")
public class IProductServiceImpl implements IProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    public ServerResponse saveOrUpdateProduct(Product product) {
        if (product != null) {
            if (StringUtils.isNotBlank(product.getSubImages())) {
                String[] subImageArray = product.getSubImages().split(",");
                if (subImageArray.length > 0) {
                    product.setMainImage(subImageArray[0]);
                }
            }
            if (product.getId() != null) {
                //更新产品
                int updateCount = productMapper.updateByPrimaryKey(product);
                if (updateCount > 0) {
                    return ServerResponse.createBySuccessMessage("更新产品成功");
                }
            } else {
                //新增产品
                int insertCount = productMapper.insert(product);
                if (insertCount > 0) {
                    return ServerResponse.createBySuccessMessage("新增产品成功");
                }
            }
        }
        return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
    }

    public ServerResponse<String> setSaleStatus(Integer productId, Integer status) {
        if (productId == null || status == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        int updateCount = productMapper.updateByPrimaryKeySelective(product);
        if (updateCount > 0) {
            return ServerResponse.createBySuccessMessage("产品状态更新成功");
        }
        return ServerResponse.createByErrorMessage("产品状态更新失败");
    }

    public ServerResponse<Object> manageProductDetail(Integer productId) {
        if (productId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if (product == null) {
            return ServerResponse.createByErrorMessage("商品下架或已删除");
        }
        ProductDetailVO productDetailVO = this.assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVO);
    }

    private ProductDetailVO assembleProductDetailVo(Product product) {
        ProductDetailVO productDetailVO = new ProductDetailVO();
        productDetailVO.setId(product.getId());
        productDetailVO.setSubtitle(product.getSubtitle());
        productDetailVO.setPrice(product.getPrice());
        productDetailVO.setMainImage(product.getMainImage());
        productDetailVO.setSubImages(product.getMainImage());
        productDetailVO.setCategoryId(product.getCategoryId());
        productDetailVO.setDetail(product.getDetail());
        productDetailVO.setName(product.getDetail());
        productDetailVO.setStatus(product.getStatus());
        productDetailVO.setStock(product.getStock());

        productDetailVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix", "http://img.happymmall.com"));

        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if (category == null) {
            productDetailVO.setParentCategory(0);
        } else {
            productDetailVO.setParentCategory(category.getParentId());
        }

        productDetailVO.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVO.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVO;
    }

    public ServerResponse getProductList(Integer pageNum, Integer pageSize) {

        //startPage--start
        //填充自己的sql逻辑
        //pageHelper--收尾
        PageHelper.startPage(pageNum, pageSize);
        List<Product> products = productMapper.selectList();
        List<ProductListVO> productListVOS = Lists.newArrayList();
        for(Product p :products){
            ProductListVO productListVO = this.assembleProductListVO(p);
            productListVOS.add(productListVO);
        }
        PageInfo pageInfo  = new PageInfo(products);
        pageInfo.setList(productListVOS);
        return ServerResponse.createBySuccess(pageInfo);
    }


    private ProductListVO assembleProductListVO(Product product) {
        ProductListVO productListVO = new ProductListVO();
        productListVO.setCategoryId(product.getCategoryId());
        productListVO.setId(product.getId());
        productListVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix", "http://img.happymmall.com"));
        productListVO.setMainImage(product.getMainImage());
        productListVO.setName(product.getName());
        productListVO.setSubtitle(product.getSubtitle());
        productListVO.setStatus(product.getStatus());
        productListVO.setPrice(product.getPrice());
        return productListVO;
    }

    public ServerResponse<PageInfo> searchProduct(String productName,Integer productId,Integer pageNum,Integer pageSize){
        PageHelper.startPage(pageNum,pageSize);
        if(productName != null) {
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<Product> products = productMapper.selectByNameAndProductId(productName,productId);
        List<ProductListVO> productListVOS = Lists.newArrayList();
        for(Product p :products){
            ProductListVO productListVO = this.assembleProductListVO(p);
            productListVOS.add(productListVO);
        }
        PageInfo pageInfo  = new PageInfo(products);
        pageInfo.setList(productListVOS);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
