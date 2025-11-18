const https = require('https');

const API_KEY = process.env.RAPIDAPI_KEY || process.env.REALTY_API_KEY || 'YOUR_RAPIDAPI_KEY';
const API_HOST = 'realty-in-us.p.rapidapi.com';

// Test 1: Search for properties
function searchProperties() {
    return new Promise((resolve, reject) => {
        const options = {
            method: 'POST',
            hostname: API_HOST,
            path: '/properties/v3/list',
            headers: {
                'content-type': 'application/json',
                'X-RapidAPI-Key': API_KEY,
                'X-RapidAPI-Host': API_HOST
            }
        };

        const req = https.request(options, function (res) {
            const chunks = [];

            res.on('data', function (chunk) {
                chunks.push(chunk);
            });

            res.on('end', function () {
                const body = Buffer.concat(chunks);
                const response = JSON.parse(body.toString());
                resolve(response);
            });
        });

        req.on('error', reject);

        // Search in Boston
        req.write(JSON.stringify({
            limit: 5,
            offset: 0,
            postal_code: "02215",
            status: ["for_sale"]
        }));

        req.end();
    });
}

// Test 2: Get property detail via v3/list with query.property_id
function getPropertyDetail(propertyId) {
    return new Promise((resolve, reject) => {
        const options = {
            method: 'POST',
            hostname: API_HOST,
            path: '/properties/v3/list',
            headers: {
                'content-type': 'application/json',
                'X-RapidAPI-Key': API_KEY,
                'X-RapidAPI-Host': API_HOST
            }
        };

        const req = https.request(options, function (res) {
            const chunks = [];

            res.on('data', function (chunk) {
                chunks.push(chunk);
            });

            res.on('end', function () {
                const body = Buffer.concat(chunks);
                const response = JSON.parse(body.toString());
                resolve(response);
            });
        });

        req.on('error', reject);

        req.write(JSON.stringify({
            query: { property_id: String(propertyId) },
            limit: 1
        }));
        req.end();
    });
}

// Test 3: Hit local service investment-data endpoint
function testLocalEndpoint(propertyId) {
    return new Promise((resolve, reject) => {
        const http = require('http');
        const url = `http://localhost:8081/api/properties/${propertyId}/investment-data`;
        http.get(url, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(new Error(`Invalid JSON from local endpoint: ${e.message}\nRaw: ${data}`));
                }
            });
        }).on('error', reject);
    });
}

// Main test
async function testAPI() {
    console.log('=================================');
    console.log('Testing Realty In US API');
    console.log('=================================\n');

    try {
        // Step 1: Search for properties
        console.log('Step 1: Searching for properties in Boston (02215)...\n');
        const searchResults = await searchProperties();
        
        console.log(`Found ${searchResults.data?.home_search?.total || 0} properties`);
        console.log(`Returned ${searchResults.data?.home_search?.results?.length || 0} results\n`);

        if (searchResults.data?.home_search?.results?.length > 0) {
            const firstProperty = searchResults.data.home_search.results[0];
            console.log('=================================');
            console.log('SEARCH RESULT - First Property Fields:');
            console.log('=================================');
            console.log(JSON.stringify(firstProperty, null, 2));
            console.log('\n');

            // List all fields
            console.log('Available fields in search result:');
            Object.keys(firstProperty).forEach(key => {
                const value = firstProperty[key];
                const type = Array.isArray(value) ? 'array' : typeof value;
                console.log(`  - ${key}: ${type}`);
            });

            // Step 2: Get detailed property info (via v3/list)
            const propertyId = firstProperty.property_id;
            console.log('\n=================================');
            console.log(`Step 2: Getting details for property ${propertyId}...`);
            console.log('=================================\n');

            const detailResult = await getPropertyDetail(propertyId);
            const listResult = detailResult?.data?.home_search?.results?.[0];
            
            if (listResult) {
                const propertyDetail = listResult; // using search result as detail proxy
                console.log('=================================');
                console.log('PROPERTY DETAIL - All Fields:');
                console.log('=================================');
                console.log(JSON.stringify(propertyDetail, null, 2));
                console.log('\n');

                console.log('Available fields in property detail:');
                Object.keys(propertyDetail).forEach(key => {
                    const value = propertyDetail[key];
                    const type = Array.isArray(value) ? 'array' : typeof value;
                    console.log(`  - ${key}: ${type}`);
                });

                // Key investment-related fields
                console.log('\n=================================');
                console.log('KEY INVESTMENT FIELDS:');
                console.log('=================================');
                const addr = propertyDetail.location?.address ?? propertyDetail.location?.address ?? {};
                console.log('Address:', addr?.line || 'N/A');
                console.log('City:', addr?.city || 'N/A');
                console.log('State:', addr?.state_code || 'N/A');
                console.log('Zip:', addr?.postal_code || 'N/A');
                console.log('List Price:', propertyDetail.list_price ? `$${Number(propertyDetail.list_price).toLocaleString()}` : 'N/A');
                console.log('Price per sqft:', propertyDetail.price_per_sqft ? `$${propertyDetail.price_per_sqft}` : 'N/A');
                console.log('Estimated Value:', propertyDetail.estimate?.estimate ? `$${Number(propertyDetail.estimate.estimate).toLocaleString()}` : 'N/A');
                console.log('Property Type:', propertyDetail.description?.type || 'N/A');
                console.log('Bedrooms:', propertyDetail.description?.beds || 'N/A');
                console.log('Bathrooms:', propertyDetail.description?.baths_full || 'N/A');
                console.log('Square Feet:', propertyDetail.description?.sqft || 'N/A');
                console.log('Year Built:', propertyDetail.description?.year_built || 'N/A');
                console.log('Lot Size:', propertyDetail.description?.lot_sqft || 'N/A');
                console.log('HOA Fee:', propertyDetail.hoa?.fee || 'N/A');
                console.log('Tax Amount:', propertyDetail.tax_history?.[0]?.tax || 'N/A');
                console.log('Tax Year:', propertyDetail.tax_history?.[0]?.year || 'N/A');
                
                // Check for rental/investment data
                console.log('\n--- Financial Data ---');
                console.log('Tax History:', propertyDetail.tax_history ? 'Available' : 'Not Available');
                console.log('Price History:', propertyDetail.price_history ? 'Available' : 'Not Available');
                console.log('Rental Estimate:', propertyDetail.rental_estimate ? 'Available' : 'Not Available');
                console.log('Days on Market:', propertyDetail.days_on_market || 'N/A');

                // Step 3: Test local endpoint
                console.log('\n=================================');
                console.log('Step 3: Testing local /investment-data endpoint...');
                console.log('=================================\n');
                try {
                    const local = await testLocalEndpoint(propertyId);
                    const info = local.propertyInfo || {};
                    console.log('Local propertyInfo summary:', {
                        propertyId: info.propertyId,
                        address: info.address,
                        city: info.city,
                        state: info.state,
                        listPrice: info.listPrice,
                        beds: info.beds,
                        baths: info.baths,
                        propertyType: info.propertyType
                    });
                } catch (e) {
                    console.log('Local endpoint test failed:', e.message);
                }
            } else {
                console.log('No property detail found from v3/list');
            }
        } else {
            console.log('No properties found in search results');
        }

    } catch (error) {
        console.error('Error:', error.message);
    }
}

testAPI();
