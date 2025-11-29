//package com.MWS.controller;
//
//import com.MWS.service.UserService;
//import com.MWS.dto.create_update.CreateUserDTO;
//import com.MWS.dto.get.GetSimpleUserDto;
//import jakarta.ws.rs.*;
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.core.Response;
//import java.util.UUID;
//
//@Path("/api/users")
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
//public class UserControllerJakarta {
//
//    private final UserService userService;
//
//    public UserControllerJakarta(UserService userService) {
//        this.userService = userService;
//    }
//
//    @POST
//    public Response createUser(CreateUserDTO dto) {
//        GetSimpleUserDto createdUser = userService.createUser(dto);
//        return Response.status(Response.Status.CREATED).entity(createdUser).build();
//    }
//
//    @GET
//    @Path("/{id}")
//    public Response getUser(@PathParam("id") UUID id) {
//        GetSimpleUserDto user = userService.getUser(id);
//        return Response.ok(user).build();
//    }
//
//    @PUT
//    @Path("/{id}")
//    public Response updateUser(@PathParam("id") UUID id, CreateUserDTO dto) {
//        GetSimpleUserDto updatedUser = userService.updateUser(id, dto);
//        return Response.ok(updatedUser).build();
//    }
//
//    @DELETE
//    @Path("/{id}")
//    public Response deleteUser(@PathParam("id") UUID id) {
//        userService.deleteUser(id);
//        return Response.noContent().build();
//    }
//}