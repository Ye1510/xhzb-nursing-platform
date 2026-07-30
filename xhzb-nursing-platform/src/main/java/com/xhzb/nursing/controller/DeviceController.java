package com.xhzb.nursing.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.service.IDeviceService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 设备管理Controller
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/nursing/device")
@Tag(name = "设备管理相关接口")
public class DeviceController extends BaseController
{

    @Autowired
    private IDeviceService deviceService;

    @GetMapping("/queryProduct/{productKey}")
    public AjaxResult queryProduct(@PathVariable String productKey){
        return deviceService.queryProduct(productKey);
    }

    @GetMapping("/allProduct")
    @Operation(summary = "查询所有产品列表")
    public AjaxResult allProduct() {
        return success(deviceService.allProduct());
    }

    @PostMapping("/syncProductList")
    @Operation(summary = "从物联网平台同步产品列表")
    public AjaxResult syncProductList() {
        deviceService.syncProductList();
        return success();
    }
    /**
     * 查询设备管理列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:list')")
    @GetMapping("/list")
    @Operation(summary = "查询设备管理列表")
    public TableDataInfo list(Device device)
    {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 注册设备
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:register')")
    @Log(title = "设备管理", businessType = BusinessType.INSERT)
    @PostMapping("/register")
    @Operation(summary = "注册设备")
    public AjaxResult register(@RequestBody Device device)
    {
        Device registeredDevice = deviceService.registerDevice(device);
        return success(registeredDevice);
    }

    /**
     * 查询设备详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:query')")
    @GetMapping("/{iotId}")
    @Operation(summary = "查询设备详细信息")
    public AjaxResult getDeviceDetail(@PathVariable String iotId)
    {
        return success(deviceService.getDeviceDetail(iotId));
    }

    /**
     * 修改设备
     * 先修改华为云IoT平台，再修改本地数据库
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:edit')")
    @Log(title = "设备管理", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改设备")
    public AjaxResult edit(@RequestBody Device device)
    {
        return success(deviceService.updateDeviceInfo(device));
    }

    /**
     * 删除设备
     * 先删除华为云IoT平台设备，再删除本地数据库
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:remove')")
    @Log(title = "设备管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除设备")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        deviceService.deleteDeviceInfo(ids);
        return success();
    }

    /**
     * 查询设备上报的属性数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:query')")
    @GetMapping("/queryServiceProperties/{iotId}")
    @Operation(summary = "查询设备上报的属性数据")
    public AjaxResult queryServiceProperties(@PathVariable String iotId)
    {
        return success(deviceService.queryServiceProperties(iotId));
    }

}
