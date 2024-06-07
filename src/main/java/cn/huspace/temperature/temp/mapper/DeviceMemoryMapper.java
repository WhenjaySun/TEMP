package cn.huspace.temperature.temp.mapper;

import cn.huspace.temperature.temp.dto.DeviceMemoryDTO;
import cn.huspace.temperature.temp.dto.DeviceTempDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceMemoryMapper {
    int insertAll(List<DeviceMemoryDTO> deviceMemoryDTOS);

    List<DeviceMemoryDTO> selectAllLimit(int limit);
}
