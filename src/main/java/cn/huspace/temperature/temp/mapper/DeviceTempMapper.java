package cn.huspace.temperature.temp.mapper;

import cn.huspace.temperature.temp.dto.DeviceTempDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceTempMapper {
    int insertAll(List<DeviceTempDTO> deviceTempDTOs);

    List<DeviceTempDTO> selectAllLimit(int limit);
}
