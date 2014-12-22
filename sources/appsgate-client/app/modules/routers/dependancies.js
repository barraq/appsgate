define([
    "app",
    "views/dependancy/graph",
	"views/dependancy/menu"
], function (App, GraphView, DependancyMenuView) {

	var DependancyRouter = {};
	// router
	DependancyRouter = Backbone.Router.extend({
		routes: {
			"dependancies": "all",
			"dependancies/all": "all",
			"dependancies/:id": "selected"
		},
		// No selected entity
		all: function () {

			// remove and unbind the current view for the menu
			if (appRouter.currentMenuView) {
				appRouter.currentMenuView.close();
				appRouter.currentMenuView = null;
			}
			if (appRouter.currentView) {
				appRouter.currentView.close();
				appRouter.currentView = null;
			}

			$("#main").html(appRouter.navbartemplate());

			// Send the request to the server to get the graph
			communicator.sendMessage({
				method: "getGraph",
				args: [],
				callId: "loadGraph",
				TARGET: "EHMI"
			});

			$("#main").append(appRouter.circlemenutemplate());

			// initialize the circle menu
			$(".controlmenu").circleMenu({
				trigger: "click",
				item_diameter: 50,
				circle_radius: 75,
				direction: 'top-right'
			});

			appRouter.navigate("#dependancies/all", {
				replace: true
			});

			$(".nav-item").removeClass("active");
			$(".dependancies-nav").addClass("active");

			$("body").i18n();

			// Once the dependancies have been created and added to the collection, show the graph
			dispatcher.once("dependanciesReady", function () {
				appRouter.showMenuView(new DependancyMenuView({
					model: dependancies.at(0)
				}));

				$(".nav-item").removeClass("active");
				$(".dependancies-nav").addClass("active");

				appRouter.showDetailsView(new GraphView({
					//                    el: $("#main"),
					model: dependancies.at(0)
				}));
			});
		},
		// One entity selected
		selected: function (idSelected) {
			// remove and unbind the current view for the menu
			if (appRouter.currentMenuView) {
				appRouter.currentMenuView.close();
				appRouter.currentMenuView = null;
			}
			if (appRouter.currentView) {
				appRouter.currentView.close();
				appRouter.currentView = null;
			}

			$("#main").html(appRouter.navbartemplate());

			// Send the request to the server to get the graph
			communicator.sendMessage({
				method: "getGraph",
				args: [],
				callId: "loadGraph",
				TARGET: "EHMI"
			});

			$("#main").append(appRouter.circlemenutemplate());

			// initialize the circle menu
			$(".controlmenu").circleMenu({
				trigger: "click",
				item_diameter: 50,
				circle_radius: 75,
				direction: 'top-right'
			});

			appRouter.navigate("#dependancies/from=" + idSelected, {
				replace: true
			});

			$(".nav-item").removeClass("active");
			$(".dependancies-nav").addClass("active");

			$("body").i18n();

			// Once the dependancies have been created and added to the collection, show the graph
			dispatcher.once("dependanciesReady", function () {

				// Initialize the dependency model puting the entity of the selected id at "root"
				dependencyLoaded = dependancies.at(0);
				dependencyLoaded.initializeRootNode(idSelected);

				appRouter.showMenuView(new DependancyMenuView({
					model: dependencyLoaded
				}));

				$(".nav-item").removeClass("active");
				$(".dependancies-nav").addClass("active");

				appRouter.showDetailsView(new GraphView({
					model: dependencyLoaded
				}));
			});
		}
	});
	return DependancyRouter;
});