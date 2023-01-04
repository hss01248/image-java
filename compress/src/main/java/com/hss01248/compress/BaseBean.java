package com.hss01248.compress;

import java.util.HashMap;
import java.util.Map;

public class BaseBean<T> {
    public String code;

    public final static String CODE_CANCEL = "cancel";
    public final static String CODE_TIMEOUT = "time out";
    public final static String CODE_JAVA_EXCEPTION = "exception";
    public final static String CODE_ILLIGAL_PARAMS = "illeagal params";

    public final static String CODE_EMPTY = "empty";
    public final static String CODE_UNLOGIN = "unLogin";

    public BaseBean setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public String msg;
    public T data;
    public Object errorData;
    public boolean success;

    public static <T> BaseBean<T> success(T bean){
        BaseBean<T> baseBean = new BaseBean<T>();
        baseBean.success = true;
        baseBean.data = bean;
        baseBean.msg = "request success";
        return baseBean;
    }
    public static BaseBean successBack(){
        BaseBean baseBean = new BaseBean();
        baseBean.success = true;
        baseBean.msg = "request success";
        return baseBean;
    }

    public static <T> BaseBean<T> empty(){
        BaseBean<T> baseBean = new BaseBean<T>();
        baseBean.success = true;
        baseBean.msg = "data is empty";
        return baseBean;
    }


    public static BaseBean error(String code,String msg){
        BaseBean baseBean = new BaseBean();
        baseBean.success = false;
        baseBean.code = code;
        baseBean.msg = msg;
        return baseBean;
    }
    public static BaseBean notExist(){
        BaseBean baseBean = new BaseBean();
        baseBean.success = false;
        baseBean.code = "not exist";
        baseBean.msg = "data not exist";
        return baseBean;
    }

    public static BaseBean alreadyExist(){
        BaseBean baseBean = new BaseBean();
        baseBean.success = false;
        baseBean.code = "already exist";
        baseBean.msg = "data already exist";
        return baseBean;
    }




    public static BaseBean success(Map<String, Object> data){
        BaseBean response = new BaseBean();
        response.success = true;
        response.data = data;
        return response;
    }

    public static Builder put(String key,Object value){

        return BaseBean.newBuilder().put(key,value);
    }

    public static  Builder  success(){
        return new Builder();
    }

    public static BaseBean exception(Throwable throwable){
        if(throwable.getCause() != null){
            throwable = throwable.getCause();
        }
        return error(CODE_JAVA_EXCEPTION,throwable.getClass().getSimpleName()+": "+throwable.getMessage());
    }
    public static BaseBean illeagalParms(String params){
        return error(CODE_ILLIGAL_PARAMS,params);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private Map<String, Object> data = new HashMap();

        private Builder() {
        }

        public Builder put(String key,Object value) {
            data .put(key, value);
            return this;
        }

        public BaseBean build() {
            return BaseBean.success(data);
        }
    }

    @Override
    public String toString() {
        return "BaseBean{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                ", errorData=" + errorData +
                ", success=" + success +
                '}';
    }
}
