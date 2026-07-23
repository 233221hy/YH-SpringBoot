package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSettings;
import cn.xfywz.guozespring.mapper.SlSettingsMapper;
import cn.xfywz.guozespring.service.admin.SlSettingsService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlSettingsServiceImpl implements SlSettingsService {
    @Autowired
    private SlSettingsMapper slSettingsMapper;
    @Override
    public Result info() {
        SlSettings slSettings = slSettingsMapper.selectById(1);
        return Result.success(slSettings);
    }

    @Override
    public Result update(SlSettings slSettings) {
        slSettingsMapper.updateById(slSettings);
        return Result.success(slSettings);
    }
}
