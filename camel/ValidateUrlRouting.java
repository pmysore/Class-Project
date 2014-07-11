package com.chacha.threeeleven.routing;

import com.chacha.camel.routing.ChaRoute;

public class ValidateUrlRouting  extends ChaRoute
{
	@Override
	public void configure() throws Exception 
	{
		from("{{311-endpoint-validUrl}}").
		to("bean:resourceValidator?method=isUrlValid").
		end();
	}
}
