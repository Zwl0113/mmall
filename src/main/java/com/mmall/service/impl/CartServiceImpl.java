package com.mmall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

@Service("iCartService")
public class CartServiceImpl implements ICartService{

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 添加购物车的商品
     * @param productId
     * @param userId
     * @param count
     * @return
     */
    public ServerResponse<CartVo> add(Integer productId, Integer userId, Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdProductId(productId, userId);
        if(cart == null){
            //该产品不在购物车中,需要新增一个商品的记录
            Cart cartItem = new Cart();
            cartItem.setProductId(productId);
            cartItem.setChecked(Const.Cart.CHECKED);
            cartItem.setQuantity(count);
            cartItem.setUserId(userId);
            cartMapper.insert(cartItem);
        }else{
            //这个产品已经在购物车中，如果产品存在，数量叠加
            int newCount = count + cart.getQuantity();
            cart.setQuantity(newCount);
            cartMapper.updateByPrimaryKey(cart);
        }
        return this.list(userId);
    }


    /**
     * 更新购物车中的商品
     * @param productId
     * @param userId
     * @param count
     * @return
     */
    public ServerResponse<CartVo> update(Integer productId, Integer userId, Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdProductId(productId, userId);
        if(cart != null){
            cart.setQuantity(count);
        }
        try{
            cartMapper.updateByPrimaryKeySelective(cart);

        }catch (Exception e){
            e.printStackTrace();
        }
        return this.list(userId);
    }


    /**
     * 在购物车中删除指定商品
     * @param userId
     * @param productIds
     * @return
     */
    public ServerResponse<CartVo> deleteProduct(Integer userId, String productIds){
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productIdList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        try{
            cartMapper.deleteByUserIdProductIds(userId,productIdList);
        }catch (Exception e){
            e.printStackTrace();
        }

        return this.list(userId);
    }

    /**
     * 查询购物车
     * @param userId
     * @return
     */
    public ServerResponse<CartVo> list(Integer userId){
        CartVo cartVo = this.getCartVOLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }


    /**
     * 单选或反选购物车商品
     * @param userId
     * @param productId
     * @param checked
     * @return
     */
    public ServerResponse<CartVo> selectOrUnSelect(Integer userId, Integer productId, Integer checked){
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
    }


    public ServerResponse<Integer> getCartProductCount(Integer userId){
        if(userId == null){
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }


    private CartVo getCartVOLimit(Integer userId){
        CartVo cartVo = new CartVo();
        BigDecimal cartTotalPrice = new BigDecimal("0");
        List<Cart> carts = cartMapper.selectCartByUserId(userId);
        List<CartProductVo> cartProductVos = Lists.newArrayList();
        for(Cart c :carts){
            CartProductVo cartProductVo = new CartProductVo();
            cartProductVo.setId(c.getId());
            cartProductVo.setProductId(c.getProductId());
            cartProductVo.setUserId(c.getUserId());
            Product product = productMapper.selectByPrimaryKey(c.getProductId());
            if(product != null) {
                cartProductVo.setProductMainImage(product.getMainImage());
                cartProductVo.setProductName(product.getName());
                cartProductVo.setProductStatus(product.getStatus());
                cartProductVo.setProductSubtitle(product.getSubtitle());
                cartProductVo.setProductStock(product.getStock());
                cartProductVo.setProductPrice(product.getPrice());
                int buyLimitCount = 0;
                if (product.getStock() >= c.getQuantity()) {
                    //库存充足
                    buyLimitCount = c.getQuantity();
                    cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                } else {
                    buyLimitCount = product.getStock();
                    cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                    //库存不足,更新购物车的库存记录
                    Cart newCart = new Cart();
                    newCart.setId(c.getId());
                    newCart.setQuantity(buyLimitCount);
                    cartMapper.updateByPrimaryKeySelective(newCart);
                }
                cartProductVo.setQuantity(buyLimitCount);
                //计算总价
                cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cartProductVo.getQuantity()));
                cartProductVo.setProductChecked(c.getChecked());
            }
            if(c.getChecked() == Const.Cart.CHECKED){
                //选中
                cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(), cartProductVo.getProductTotalPrice().doubleValue());
            }
            cartProductVos.add(cartProductVo);
        }
        cartVo.setCartProductVoList(cartProductVos);
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setAllCecked(this.getAllCheckedStatus(userId));
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return cartVo;
    }

    private boolean getAllCheckedStatus(Integer userId){
        if(userId == null){
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }



}
