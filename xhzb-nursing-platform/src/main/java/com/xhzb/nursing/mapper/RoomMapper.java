package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.Room;
import com.xhzb.nursing.domain.vo.RoomVo;
import com.xhzb.nursing.domain.vo.RoomOneVo;
import com.xhzb.nursing.domain.vo.BedVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 房间Mapper接口
 * 
 * @author ruoyi
 * @date 2025-03-28
 */
@Mapper
public interface RoomMapper extends BaseMapper<Room>
{
    /**
     * 查询房间
     * 
     * @param id 房间主键
     * @return 房间
     */
    public Room selectRoomById(Long id);

    /**
     * 查询房间列表
     * 
     * @param room 房间
     * @return 房间集合
     */
    public List<Room> selectRoomList(Room room);

    /**
     * 新增房间
     * 
     * @param room 房间
     * @return 结果
     */
    public int insertRoom(Room room);

    /**
     * 修改房间
     * 
     * @param room 房间
     * @return 结果
     */
    public int updateRoom(Room room);

    /**
     * 删除房间
     * 
     * @param id 房间主键
     * @return 结果
     */
    public int deleteRoomById(Long id);

    /**
     * 批量删除房间
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRoomByIds(Long[] ids);

    List<RoomVo> selectByFloorId(Long floorId);

    List<RoomVo> selectByFloorIdWithNur(Long floorId);

    RoomOneVo selectRoomOneById(Long id);

    /**
     * 查询指定楼层下所有有设备的房间及其房间级设备
     */
    List<RoomVo> selectRoomsWithDevicesByFloorId(Long floorId);

    /**
     * 查询指定房间下的床位、入住老人、床位级设备
     */
    List<BedVo> selectBedsWithDevicesByRoomIds(@Param("roomIds") List<Long> roomIds);

}
