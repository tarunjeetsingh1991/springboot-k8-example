package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.entity.Order;
import com.example.repository.OrderRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    public Order addOrder(Order order){
       return repository.save(order);
    }

    public List<Order> getOrders(){
        return repository.findAll();
    }

    public Order getOrderById(int id){
        return repository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("Invalid id : "+id));
    }
    
    public List<Order> sortOrders(){
        return repository.findAll().stream()
        		.sorted((s1,s2) -> s1.getName().compareTo(s2.getName()))
        		.collect(Collectors.toList());
    }
    
    public void deleteOrder(int id)
    {
    	Order ord = getOrderById(id);
    	repository.delete(ord);
    }

}