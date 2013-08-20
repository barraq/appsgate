{
	programName : "LightsOn",
	seqParameters : [
	],
	author : "Bob",
	target : "Alice",
	deamon : "true",
	seqDefinitions : [
	],
	seqRules : [
		[
			{
				type : "NodeWhen",
				events : [
					{
						type : "NodeEvent",
						sourceType : "device",
						sourceId : "ENO2840bb",
						eventName : "inserted",
						eventValue : true
					}
				],
				seqRulesThen : [
					[
						{
							type : "NodeAction",
							targetType : "device",
							targetId : "194.199.23.136-1",
							methodName : "On",
							args : [
							]
						 },
					     {
						     type : "NodeAction",
						     targetType : "device",
						     targetId : "194.199.23.136-2",
					             methodName : "On",
						     args : [
						     ]
					     },
				    	     {
						     type : "NodeAction",
						     targetType : "device",
						     targetId : "194.199.23.136-3",
					             methodName : "On",
						     args : [
						     ]
					     },
				    	     {
						     type : "NodeAction",
						     targetType : "device",
						     targetId : "194.199.23.136-4",
					             methodName : "On",
						     args : [
						     ]
					     },
				    	     {
						     type : "NodeAction",
						     targetType : "device",
						     targetId : "194.199.23.136-5",
					             methodName : "On",
						     args : [
						     ]
					     },
				    	     {
						     type : "NodeAction",
						     targetType : "device",
						     targetId : "194.199.23.136-6",
					             methodName : "On",
						     args : [
						     ]
					     }
					]
				]
			}
		]
	]
}