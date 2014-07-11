package com.chacha.threeeleven.routing;

import com.chacha.camel.routing.ChaRoute;
import com.chacha.document.AnswerSent;

public class Message311Routing  extends ChaRoute
{
	@Override
	public void configure() throws Exception 
	{
		from("{{311-endpoint-start}}").
		beanRef("startQuery", "receive").
		choice().
			when(body().isNotNull()).
				setHeader("routingSlipHeader", simple("${properties:311-endpoint-routingSlipHeader}")).
				setHeader("completionRoutingSlip", simple("${properties:311-endpoint-end}")).
				to("next:routingSlipHeader").
		end().
		end();
		
		from("{{311-endpoint-end}}").
			choice().
				when(simple("${in.body} is 'com.chacha.document.annotated.AlertsMessage'")).
					convertBodyTo(AnswerSent.class).
					beanRef("endQuery", "receive").
				when(simple("${in.body} is 'com.chacha.document.annotated.AutoAnswerPatternResponse'")).
					convertBodyTo(AnswerSent.class).
					beanRef("endQuery", "receive").
				when(simple("${in.body} is 'com.chacha.document.GuidedWorkComplete'")).
					convertBodyTo(AnswerSent.class).
					beanRef("endQuery", "receive").
				otherwise().
					beanRef("endQuery", "receive").
			end().
		end();
	}
}
