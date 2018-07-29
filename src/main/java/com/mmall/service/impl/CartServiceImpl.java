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
import com.mmall.vo.CartProductVO;
import com.mmall.vo.CartVO;
import net.sf.jsqlparser.schema.Server;
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
    public ServerResponse<CartVO> add(Integer productId,Integer userId,Integer count){
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
    public ServerResponse<CartVO> update(Integer productId,Integer userId,Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdProductId(productId, userId);
        if(cart == null){
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKey(cart);
        return this.list(userId);
    }


    /**
     * 在购物车中删除指定商品
     * @param userId
     * @param productIds
     * @return
     */
    public ServerResponse<CartVO> deleteProduct(Integer userId,String productIds){
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productIdList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        cartMapper.deleteByUserIdProductIds(userId,productIdList);
        return this.list(userId);
    }

    /**
     * 查询购物车
     * @param userId
     * @return
     */
    public ServerResponse<CartVO> list(Integer userId){
        CartVO cartVO = this.getCartVOLimit(userId);
        return ServerResponse.createBySuccess(cartVO);
    }


    /**
     * 全选或反选购物车商品
     * @param userId
     * @param checked
     * @return
     */
    public ServerResponse<CartVO> selectOrUnSelect(Integer userId,Integer checked){
        cartMapper.checkedOrUncheckedProduct(userId,null,checked);
        return this.list(userId);
    }

    /**
     * 单选或反选购物车商品
     * @param userId
     * @param productId
     * @param checked
     * @return
     */
    public ServerResponse<CartVO> selectOrUnSelect(Integer userId,Integer productId,Integer checked){
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
    }


    public ServerResponse<Integer> getCartProductCount(Integer userId){
        if(userId == null){
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }


    private CartVO getCartVOLimit(Integer userId){
        CartVO cartVO = new CartVO();
        BigDecimal cartTotalPrice = new BigDecimal("0");
        List<Cart> carts = cartMapper.selectCartByUserID(userId);
        List<CartProductVO> cartProductVOS = Lists.newArrayList();
        for(Cart c :carts){
            CartProductVO cartProductVO = new CartProductVO();
            cartProductVO.setId(c.getId());
            cartProductVO.setProductId(c.getProductId());
            cartProductVO.setUserId(c.getUserId());
            Product product = productMapper.selectByPrimaryKey(c.getId());
            if(product != null) {
                cartProductVO.setProductMainImage(product.getMainImage());
                cartProductVO.setProductName(product.getName());
                cartProductVO.setProductStatus(product.getStatus());
                cartProductVO.setProductSubtitle(product.getSubtitle());
                cartProductVO.setProductStock(product.getStock());
                cartProductVO.setProductPrice(product.getPrice());
                int buyLimitCount = 0;
                if (product.getStock() >= c.getQuantity()) {
                    //库存充足
                    buyLimitCount = c.getQuantity();
                    cartProductVO.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                } else {
                    buyLimitCount = product.getStock();
                    cartProductVO.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                    //库存不足,更新购物车的库存记录
                    Cart newCart = new Cart();
                    newCart.setId(c.getId());
                    newCart.setQuantity(buyLimitCount);
                    cartMapper.updateByPrimaryKey(newCart);
                }
                cartProductVO.setQuantity(buyLimitCount);
                //计算总价
                cartProductVO.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cartProductVO.getQuantity()));
                cartProductVO.setProductChecked(c.getChecked());
            }
            if(c.getChecked() == Const.Cart.CHECKED){
                //选中
                cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVO.getProductTotalPrice().doubleValue());
            }
            cartProductVOS.add(cartProductVO);
        }
        cartVO.setCartProductVOList(cartProductVOS);
        cartVO.setCartTotalPrice(cartTotalPrice);
        cartVO.setAllCecked(this.getAllCheckedStatus(userId));
        cartVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return cartVO;
    }

    private boolean getAllCheckedStatus(Integer userId){
        if(userId == null){
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }



}
