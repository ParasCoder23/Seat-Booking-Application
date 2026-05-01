// Frontend Booking Test Script
// Run this in browser console at http://localhost:3000

async function testBookingFlow() {
    console.log('🎯 Testing Seat Booking Flow...');

    const API_BASE = 'http://localhost:8080/api';

    try {
        // Step 1: Login
        console.log('1. Logging in...');
        const loginResponse = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'admin', password: 'admin123' })
        });

        if (!loginResponse.ok) {
            throw new Error(`Login failed: ${loginResponse.statusText}`);
        }

        const loginData = await loginResponse.json();
        const token = loginData.token;
        console.log('✅ Login successful, token received');

        // Step 2: Get available seats
        console.log('2. Getting available seats...');
        const seatsResponse = await fetch(`${API_BASE}/seats/available`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!seatsResponse.ok) {
            throw new Error(`Failed to get seats: ${seatsResponse.statusText}`);
        }

        const seats = await seatsResponse.json();
        console.log(`✅ Found ${seats.length} available seats`);

        if (seats.length === 0) {
            console.error('❌ No available seats found!');
            return;
        }

        const testSeat = seats[0];
        console.log(`📍 Testing with seat: ${testSeat.seatNumber} (ID: ${testSeat.id})`);

        // Step 3: Lock the seat
        console.log('3. Locking seat...');
        const lockResponse = await fetch(`${API_BASE}/bookings/lock/${testSeat.id}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!lockResponse.ok) {
            const errorText = await lockResponse.text();
            throw new Error(`Failed to lock seat: ${lockResponse.statusText} - ${errorText}`);
        }

        const lockData = await lockResponse.json();
        console.log('✅ Seat locked:', lockData.message);

        // Step 4: Create booking
        console.log('4. Creating booking...');
        const bookingData = {
            seatId: testSeat.id,
            startTime: '2026-03-26T09:00:00',
            endTime: '2026-03-26T17:00:00'
        };

        const bookingResponse = await fetch(`${API_BASE}/bookings`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(bookingData)
        });

        if (!bookingResponse.ok) {
            const errorText = await bookingResponse.text();
            throw new Error(`Failed to create booking: ${bookingResponse.statusText} - ${errorText}`);
        }

        const booking = await bookingResponse.json();
        console.log('✅ Booking created successfully!');
        console.log(`📅 Booking ID: ${booking.id}`);
        console.log(`💺 Seat: ${booking.seatNumber}`);
        console.log(`⏰ Time: ${booking.startTime} to ${booking.endTime}`);
        console.log(`📊 Status: ${booking.status}`);

        // Step 5: Verify booking
        console.log('5. Verifying booking...');
        const myBookingsResponse = await fetch(`${API_BASE}/bookings/my`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (myBookingsResponse.ok) {
            const myBookings = await myBookingsResponse.json();
            const foundBooking = myBookings.find(b => b.id === booking.id);
            if (foundBooking) {
                console.log('✅ Booking verified in user bookings');
            } else {
                console.warn('⚠️ Booking not found in user bookings');
            }
        }

        console.log('🎉 Booking flow test completed successfully!');

    } catch (error) {
        console.error('❌ Booking flow test failed:', error.message);

        // Additional debugging
        console.log('🔍 Running additional diagnostics...');

        try {
            const healthResponse = await fetch(`${API_BASE}/health`);
            if (healthResponse.ok) {
                const health = await healthResponse.json();
                console.log('🏥 Health check:', health);
            }
        } catch (e) {
            console.log('❌ Health check failed:', e.message);
        }
    }
}

// Test booking on behalf functionality (Admin/Manager only)
async function testBookingOnBehalf() {
    console.log('👨‍💼 Testing Booking On Behalf Functionality...');

    const API_BASE = 'http://localhost:8080/api';

    try {
        // Step 1: Login as admin
        console.log('1. Logging in as admin...');
        const loginResponse = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'admin', password: 'admin123' })
        });

        const loginData = await loginResponse.json();
        const token = loginData.token;
        console.log('✅ Admin login successful');

        // Step 2: Get employees list
        console.log('2. Getting employees list...');
        const employeesResponse = await fetch(`${API_BASE}/users/employees`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const employees = await employeesResponse.json();
        console.log(`✅ Found ${employees.length} employees`);

        const employee = employees.find(emp => emp.role === 'EMPLOYEE');
        if (!employee) {
            console.error('❌ No employee found to test with');
            return;
        }

        console.log(`📋 Selected employee: ${employee.username} (ID: ${employee.id})`);

        // Step 3: Get available seats
        const seatsResponse = await fetch(`${API_BASE}/seats/available`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const seats = await seatsResponse.json();
        if (seats.length === 0) {
            console.error('❌ No available seats found');
            return;
        }

        const testSeat = seats[0];
        console.log(`💺 Selected seat: ${testSeat.seatNumber} (ID: ${testSeat.id})`);

        // Step 4: Lock the seat
        console.log('3. Locking seat...');
        await fetch(`${API_BASE}/bookings/lock/${testSeat.id}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        console.log('✅ Seat locked');

        // Step 5: Create booking on behalf
        console.log('4. Creating booking on behalf of employee...');
        const bookingData = {
            seatId: testSeat.id,
            startTime: '2026-03-27T10:00:00',
            endTime: '2026-03-27T18:00:00',
            bookedForUserId: employee.id
        };

        const bookingResponse = await fetch(`${API_BASE}/bookings`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(bookingData)
        });

        if (!bookingResponse.ok) {
            const errorText = await bookingResponse.text();
            throw new Error(`Failed to create booking: ${errorText}`);
        }

        const booking = await bookingResponse.json();
        console.log('✅ Booking on behalf created successfully!');
        console.log(`📅 Booking ID: ${booking.id}`);
        console.log(`👤 Booked for: ${booking.bookedForUsername}`);
        console.log(`👨‍💼 Booked by: ${booking.bookedByUsername}`);
        console.log(`💺 Seat: ${booking.seatNumber}`);
        console.log(`⏰ Time: ${booking.startTime} to ${booking.endTime}`);

        console.log('🎉 Booking on behalf test completed successfully!');

    } catch (error) {
        console.error('❌ Booking on behalf test failed:', error.message);
    }
}

// Helper function to test Redis connectivity
async function testRedisConnection() {
    const API_BASE = 'http://localhost:8080/api';

    try {
        const response = await fetch(`${API_BASE}/debug/booking/redis-status`);
        if (response.ok) {
            const data = await response.json();
            console.log('💾 Redis Status:', data);
        } else {
            console.error('❌ Redis status check failed');
        }
    } catch (error) {
        console.error('❌ Redis connection test failed:', error.message);
    }
}

// Helper function to reset locks
async function resetLocks() {
    const API_BASE = 'http://localhost:8080/api';

    try {
        const response = await fetch(`${API_BASE}/debug/booking/reset-locks`, {
            method: 'POST'
        });
        if (response.ok) {
            const data = await response.json();
            console.log('🔄 Locks reset:', data);
        } else {
            console.error('❌ Failed to reset locks');
        }
    } catch (error) {
        console.error('❌ Lock reset failed:', error.message);
    }
}

console.log('🛠️ Booking Test Functions Available:');
console.log('- testBookingFlow() - Test complete booking process');
console.log('- testBookingOnBehalf() - Test admin/manager booking for employees');
console.log('- testRedisConnection() - Test Redis connectivity');
console.log('- resetLocks() - Reset all Redis locks');
console.log('');
console.log('💡 Run testBookingFlow() to start testing');
console.log('💡 Run testBookingOnBehalf() to test booking on behalf feature');

// Auto-run the test
// testBookingFlow();
