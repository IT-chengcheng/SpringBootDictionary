package sample.test.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author:chengcheng
 * @date:2020.11.15
 */
@RestController
public class TestController {

	@GetMapping("test")
	public String test(){
		System.out.println("进来了");
		return "zhangcc";
	}
}
