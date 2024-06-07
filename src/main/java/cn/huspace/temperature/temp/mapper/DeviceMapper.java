package cn.huspace.temperature.temp.mapper;

import cn.huspace.temperature.temp.dto.DeviceDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceMapper {
    List<DeviceDTO> selectAll();
}
