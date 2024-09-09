package idv.tia201.g1.order.service.impl;

import idv.tia201.g1.core.entity.UserAuth;
import idv.tia201.g1.core.utils.UserHolder;
import idv.tia201.g1.order.dao.OrderDao;
import idv.tia201.g1.order.dao.OrderDetailDao;
import idv.tia201.g1.order.dao.OrderResidentsDao;
import idv.tia201.g1.order.dto.CreateOrderRequest;
import idv.tia201.g1.order.dto.UpdateOrderRequest;
import idv.tia201.g1.order.entity.Order;
import idv.tia201.g1.order.entity.OrderDetail;
import idv.tia201.g1.order.entity.OrderResidents;
import idv.tia201.g1.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static idv.tia201.g1.core.utils.Constants.ROLE_USER;

@Service
public class OrderServiceImpl implements OrderService {

    private static final double SERVICE_FEE_PERCENT = 0.10; // 5%
    private static final double TAX_PERCENT = 0.05; // 10%

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private OrderDetailDao orderDetailDao;
    @Autowired
    private OrderResidentsDao orderResidentsDao;
    @Override
    public Order createOrder(CreateOrderRequest createOrderRequest) {
        UserAuth user = UserHolder.getUser();
        if (user == null || !ROLE_USER.equals(user.getRole())) {
            throw new IllegalStateException("未登入或是身分不符合");
        }
        //已確定身分為顧客!獲得顧客id
        Integer customId = user.getId();

        Order order = new Order();
        //訂單資訊
        order.setUserId(customId); //這張訂單屬於誰的
        order.setPayStatus("未付款");
        order.setStartDate(createOrderRequest.getBeginDate());
        order.setEndDate((createOrderRequest.getEndDate()));
        Order save = orderDao.save(order);
        Integer orderId = save.getOrderId();
        List<OrderDetail> orderDetails = new LinkedList<>();

        //處理房型列表:
        // 前端傳資料近來到ProductList,
        // 從ProductList中取得一個Product,
        // 而Product中含有orderId, productId和count
        List<CreateOrderRequest.Product> requestProductList = createOrderRequest.getProductList();
        for (CreateOrderRequest.Product requestProduct : requestProductList) {

            Integer productId = requestProduct.getProductId();
            Integer count = requestProduct.getCount();

            List<Date> datesBetween = getDatesBetween(order.getStartDate(), order.getEndDate());
            for (Date date : datesBetween) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setOrderId(orderId);
                orderDetail.setProductId(productId);
                orderDetail.setQuantity(count);
                orderDetail.setBookedDate(date);

                OrderDetail save1 = orderDetailDao.save(orderDetail);
                orderDetails.add(save1);
            }
        }

        // TODO: 處理優惠券 以及 金額
        //  計算價格 => 根據日期以及商品編號計算總金額 稅金 服務費...等
        //  更新訂單資訊
        Integer companyId = createOrderRequest.getCompanyId();

        Integer dailyPrice = orderDao.calculateTotalPrice(orderId);
        List<Double> discount = getDiscountByCompanyIdBetweenStartDateAnEndDate(companyId, save.getStartDate(), save.getEndDate());

        // 計算全價
        int fullPrice = dailyPrice * discount.size();
        // 計算折扣價
        int discountedPrice = calculateTotalDiscountedPrice(dailyPrice, discount);

        // 計算稅金與服務費
        int serviceFee = (int) Math.round(discountedPrice * SERVICE_FEE_PERCENT);
        int tax = (int) Math.round(discountedPrice * TAX_PERCENT);

        // 計算實際價格
        int actualPrice = discountedPrice + serviceFee + tax;

        save.setFullPrice(fullPrice);
        save.setServiceFee(serviceFee);
        save.setTax(tax);
        save.setDiscount(fullPrice - discountedPrice);
        save.setActualPrice(actualPrice);

        return save;

    }


    @Override
    public Order updateOrder(Integer orderId, UpdateOrderRequest request) {
        //檢查參數格式
        validateUpdateOrderRequest(request);

        //檢查是否登入
        UserAuth user = UserHolder.getUser();
        if (user == null || !ROLE_USER.equals(user.getRole())) {
            //代表這張訂單代表使用者未登入!!
            throw new IllegalStateException("使用者未登入!!");
        }
        //驗證order id是否有效
        Order order = orderDao.findByOrderId(orderId);
        //檢查這份order是否屬於登入者
        if (order == null || !order.getUserId().equals(user.getId())) {
            //代表這張訂單不屬於登入者!!
            throw new IllegalStateException("訂單不屬於登入者!!");
        }

        OrderResidents orderResident = new OrderResidents();
        orderResident.setOrderId(orderId);
        orderResident.setCountry(request.getCountry());
        orderResident.setFirstName(request.getFirstName());
        orderResident.setLastName(request.getLastName());
        orderResident.setEmail(request.getEmail());
        orderResident.setTel(request.getPhone());
        order.setOrderNotes(request.getRemark());

        Order save = orderDao.save(order);
        orderResidentsDao.save(orderResident);


        return save;
    }

    private static void validateUpdateOrderRequest(UpdateOrderRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
    }

    @Override
    public List<Order> getOrdersByUserId(Integer userId) {
        return orderDao.findByUserId(userId);
    }

    @Override
    public List<Order> getOrdersByCompanyId(Integer companyId) {
        return orderDao.findByCompanyId(companyId);
    }

    @Override
    public List<Order> getOrders() {
        return orderDao.findAll();
    }

    private List<Double> getDiscountByCompanyIdBetweenStartDateAnEndDate(Integer companyId, Date startDate, Date endDate) {
        long daysBetween = getDaysBetween(startDate, endDate);
        List<Double> res = orderDao.getDiscountByCompanyIdBetweenStartDateAnEndDate(companyId, startDate, endDate);
        while (res.size() < daysBetween) {
            res.add(1.0);
        }
        return res;
    }

    private static long getDaysBetween(Date startDate, Date endDate) {
        LocalDate startLocalDate = startDate.toLocalDate();
        LocalDate endLocalDate = endDate.toLocalDate();
        return ChronoUnit.DAYS.between(startLocalDate, endLocalDate);
    }

    private static int calculateTotalDiscountedPrice(int dailyPrice, List<Double> discounts) {
        double totalDiscountedPrice = 0.0;

        for (Double discount : discounts) {
            totalDiscountedPrice += dailyPrice * discount;
        }

        return (int) Math.round(totalDiscountedPrice);
    }

    private static List<Date> getDatesBetween(Date startDate, Date endDate) {
        List<Date> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        while (calendar.getTime().before(endDate)) {
            dates.add(new Date(calendar.getTimeInMillis()));
            calendar.add(Calendar.DATE, 1);
        }

        return dates;
    }
}
