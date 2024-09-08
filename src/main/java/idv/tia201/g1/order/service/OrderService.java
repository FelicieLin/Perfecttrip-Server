package idv.tia201.g1.order.service;

import idv.tia201.g1.order.dto.CreateOrderRequest;
import idv.tia201.g1.order.entity.Order;
import org.hibernate.sql.Update;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderService {

    //先定義一個方法,為了選擇客房!
    @Transactional
    Order createOrder(CreateOrderRequest createOrderRequest);

    //確定客房資訊
    Order updateOrder(Order order);

    //根據UserId取得訂單列表
    List<Order> getOrdersByUserId(Integer userId);

//    //根據CompanyId取得訂單列表
    List<Order> getOrdersByCompanyId(Integer companyId);

    //Admin取得所有訂單列表
    List<Order> getOrders( );

    //正式結帳訂單 , 將此會員以通過的課程訂單狀態改為已結帳

}
