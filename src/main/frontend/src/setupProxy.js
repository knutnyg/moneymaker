const createProxyMiddleware = require("http-proxy-middleware");

module.exports = function (app) {
    app.use(createProxyMiddleware(
            '/api/app/state/ws', {
            target: "http://localhost:8020/",
            ws: true,
        })
    );

    app.use(createProxyMiddleware('/api', {
            target: "http://localhost:8020/",
        })
    )
};
