package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import net.sf.jsqlparser.schema.Server;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        //todo 密码登录
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username, md5Password);
        if(user == null){
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功",user);
    }

    public ServerResponse<String> register(User user){
        ServerResponse<String> validResponse = this.checkValid(user.getUsername(), Const.USERNAME);
        if(!validResponse.isSuccess()){
            return validResponse;
        }
        int resultCount = userMapper.checkEmail(user.getEmail());
        if(resultCount > 0){
            return ServerResponse.createByErrorMessage("email已存在");
        }
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        resultCount = userMapper.insert(user);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccess("注册成功");
    }

    public ServerResponse<String> checkValid(String str,String type){
        if(StringUtils.isNotBlank(type)){
            //开始校验
            if(Const.USERNAME.equals(type)){
                int resultCount = userMapper.checkUsername(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if(Const.EMAIL.equals(type)){
                int resultCount = userMapper.checkEmail(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        }else{
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccess("校验成功");
    }

    public ServerResponse selectQuestion(String username){
        ServerResponse<String> validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if(StringUtils.isNotBlank(question)){
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    public ServerResponse<String> checkAnswer(String username,String question,String answer){
        int resultCount = userMapper.checkAnswer(username,question,answer);
        if(resultCount > 0){
            String forgettoken= UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgettoken);
            return ServerResponse.createBySuccess(forgettoken);
        }
        return ServerResponse.createByErrorMessage("问题答案错误");
    }

    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken){
        if(!StringUtils.isNotBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("参数错误，token需要传递");
        }
        ServerResponse<String> validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token无效或过期");
        }
        if(StringUtils.equals(forgetToken,token)){
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUsername(username, md5Password);
            if(rowCount > 0){
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        }else{
            return ServerResponse.createByErrorMessage("token错误，请重新获取修改密码的token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");
    }

    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew,User user){
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("旧密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0){
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    public ServerResponse<User> updateInformation(User user){
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if(resultCount > 0){
            return ServerResponse.createByErrorMessage("email已存在，请更换其他email再尝试更新");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
        }
        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    public ServerResponse<User> getInformation(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null){
            return ServerResponse.createByErrorMessage("未找到该用户");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    /**
     * 校验是否是管理员
     * @param user
     * @return
     */
    public ServerResponse checkAdminRole(User user){
        if(user != null && user.getRole() == Const.Role.ROLE_ADMIN){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
}
