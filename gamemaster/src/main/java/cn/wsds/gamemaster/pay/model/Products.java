package cn.wsds.gamemaster.pay.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;

/**
 * Created by Administrator on 2017-03-07.
 */
public class Products {
    private final List<ProductDetail> products;

    public Products(List<ProductDetail> productList) {
        this.products = productList;
    }

    public List<ProductDetail> getProductList() {
        return products;
    }

    public static Products deSerialer(String jsonStr) {
        try {
            return new Gson().fromJson(jsonStr, Products.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
