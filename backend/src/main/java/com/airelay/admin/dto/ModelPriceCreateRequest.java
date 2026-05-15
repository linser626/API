package com.airelay.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ModelPriceCreateRequest {

    @NotBlank(message = "模型ID不能为空")
    private String modelId;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotNull(message = "输入价格不能为空")
    private BigDecimal inputPrice;

    @NotNull(message = "输出价格不能为空")
    private BigDecimal outputPrice;

    private BigDecimal priceMultiplier;

    private Integer status;
}
