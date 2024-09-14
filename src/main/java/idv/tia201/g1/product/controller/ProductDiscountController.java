package idv.tia201.g1.product.controller;

import idv.tia201.g1.product.dto.AddDiscountRequest;
import idv.tia201.g1.product.entity.ProductDiscount;
import idv.tia201.g1.product.service.ProductDiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/discount")
@CrossOrigin("http://localhost:5173")
public class ProductDiscountController {

    @Autowired
    ProductDiscountService productDiscountService;

    // 測試成功
    // 添加新的優惠
    @PostMapping("/add")
    public ResponseEntity<ProductDiscount> addDiscount(@RequestBody AddDiscountRequest addDiscountRequest) {
        // 確保 startDate 和 endDate 不為 null
        if (addDiscountRequest.getStartDate() == null || addDiscountRequest.getEndDate() == null) {
            throw new IllegalArgumentException("開始日期和結束日期不能為空！");
        }

        ProductDiscount productDiscount = new ProductDiscount();
        productDiscount.setDiscountTitle(addDiscountRequest.getDiscountTitle());
        productDiscount.setDiscountRate(Float.valueOf(addDiscountRequest.getDiscountRate()));
        productDiscount.setStartDateTime(Timestamp.valueOf(addDiscountRequest.getStartDate().atStartOfDay()));
        productDiscount.setEndDateTime(Timestamp.valueOf(addDiscountRequest.getEndDate().atStartOfDay()));

        ProductDiscount savedDiscount = productDiscountService.addProductDiscount(productDiscount);
        return ResponseEntity.ok(savedDiscount);
    }

    // 測試成功
    // 根據公司ID (company_id) 來查詢優惠
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ProductDiscount>> getByCompanyId(@PathVariable Integer companyId) {
        List<ProductDiscount> discounts = productDiscountService.getByCompanyId(companyId);

        if (discounts == null || discounts.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(discounts);
        }
    }

    @PutMapping("/update/{discountId}")
    public ResponseEntity<ProductDiscount> updateDiscount(@PathVariable Integer discountId, @RequestBody ProductDiscount productDiscount) {
        ProductDiscount updated = productDiscountService.updateDiscount(discountId, productDiscount);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(updated);
        }
    }

    @DeleteMapping("/delete/{discountId}")
    public void deleteDiscount(@PathVariable Integer discountId) {
        productDiscountService.deleteProductDiscount(discountId);
    }
}
