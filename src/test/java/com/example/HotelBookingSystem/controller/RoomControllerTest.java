package com.example.HotelBookingSystem.controller;

import com.example.HotelBookingSystem.entity.BookedRoom;
import com.example.HotelBookingSystem.entity.Room;
import com.example.HotelBookingSystem.exception.ResourceNotFoundException;
import com.example.HotelBookingSystem.repository.UserRepository;
import com.example.HotelBookingSystem.response.RoomResponse;
import com.example.HotelBookingSystem.security.jwt.AuthTokenFilter;
import com.example.HotelBookingSystem.security.jwt.JwtUtils;
import com.example.HotelBookingSystem.service.BookingService;
import com.example.HotelBookingSystem.service.IRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IRoomService roomService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private com.example.HotelBookingSystem.security.user.HotelUserDetailsService hotelUserDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testAddNewRoom() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "room.jpg",
                "image/jpeg", "dummy image".getBytes());

        Room room = new Room();
        room.setId(1L);
        room.setRoomType("Deluxe");
        room.setRoomPrice(BigDecimal.valueOf(2000));

        Mockito.when(roomService.addNewRoom(any(), anyString(), any())).thenReturn(room);

        mockMvc.perform(multipart("/rooms/add/new-room")
                        .file(photo)
                        .param("roomType", "Deluxe")
                        .param("roomPrice", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomType").value("Deluxe"))
                .andExpect(jsonPath("$.roomPrice").value(2000));
    }

    @Test
    @WithMockUser
    void testGetRoomTypes() throws Exception {
        Mockito.when(roomService.getAllRoomTypes()).thenReturn(Arrays.asList("Deluxe", "Suite"));

        mockMvc.perform(get("/rooms/room/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Deluxe"))
                .andExpect(jsonPath("$[1]").value("Suite"));
    }

    @Test
    @WithMockUser
    void testGetAllRooms() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setRoomType("Deluxe");
        room.setRoomPrice(BigDecimal.valueOf(1500));
        room.setPhoto("photo".getBytes());

        Mockito.when(roomService.getAllRooms()).thenReturn(Collections.singletonList(room));
        Mockito.when(roomService.getRoomPhotoByRoomId(1L)).thenReturn("photo".getBytes());
        Mockito.when(bookingService.getAllBookingsByRoomId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rooms/all-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomType").value("Deluxe"))
                .andExpect(jsonPath("$[0].photo").value(Base64.encodeBase64String("photo".getBytes())));
    }

    @Test
    @WithMockUser
    void testDeleteRoom() throws Exception {
        Mockito.doNothing().when(roomService).deleteRoom(1L);

        mockMvc.perform(delete("/rooms/delete/room/{roomId}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void testUpdateRoom() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "room.jpg",
                "image/jpeg", "updated".getBytes());

        Room room = new Room();
        room.setId(1L);
        room.setRoomType("Suite");
        room.setRoomPrice(BigDecimal.valueOf(3000));
        room.setPhoto("updated".getBytes());

        Mockito.when(roomService.getRoomPhotoByRoomId(1L)).thenReturn("updated".getBytes());
        Mockito.when(roomService.updateRoom(eq(1L), anyString(), any(), any())).thenReturn(room);
        Mockito.when(bookingService.getAllBookingsByRoomId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(multipart("/rooms/update/{roomId}", 1L)
                        .file(photo)
                        .param("roomType", "Suite")
                        .param("roomPrice", "3000")
                        .with(request -> {
                            request.setMethod("PUT"); // multipart defaults to POST
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomType").value("Suite"))
                .andExpect(jsonPath("$.roomPrice").value(3000));
    }

    @Test
    @WithMockUser
    void testGetRoomById_Found() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setRoomType("Deluxe");
        room.setRoomPrice(BigDecimal.valueOf(1200));
        room.setPhoto("room".getBytes());

        Mockito.when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        Mockito.when(bookingService.getAllBookingsByRoomId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rooms/room/{roomId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomType").value("Deluxe"));
    }

    @Test
    @WithMockUser
    void testGetRoomById_NotFound() throws Exception {
        Mockito.when(roomService.getRoomById(99L)).thenThrow(new ResourceNotFoundException("Room not found"));

        assertThrows(ResourceNotFoundException.class, () -> {
            try {
                mockMvc.perform(get("/rooms/room/{roomId}", 99L));
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @WithMockUser
    void testGetAvailableRooms_NoContent() throws Exception {
        Mockito.when(roomService.getAvailableRoomsDto(any(), any(), anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rooms/available-rooms")
                        .param("checkInDate", LocalDate.now().toString())
                        .param("checkOutDate", LocalDate.now().plusDays(2).toString())
                        .param("roomType", "Deluxe"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void testGetAvailableRooms_WithRooms() throws Exception {
        Room room = new Room();
        room.setId(1L);
        room.setRoomType("Suite");
        room.setRoomPrice(BigDecimal.valueOf(5000));
        room.setPhoto("photo".getBytes());

        Mockito.when(roomService.getAvailableRoomsDto(any(), any(), anyString()))
                .thenReturn(Collections.singletonList(room));
        Mockito.when(roomService.getRoomPhotoByRoomId(1L)).thenReturn("photo".getBytes());
        Mockito.when(bookingService.getAllBookingsByRoomId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rooms/available-rooms")
                        .param("checkInDate", LocalDate.now().toString())
                        .param("checkOutDate", LocalDate.now().plusDays(2).toString())
                        .param("roomType", "Suite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomType").value("Suite"));
    }
}
