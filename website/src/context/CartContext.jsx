import React, { createContext, useContext, useReducer, useEffect } from 'react';

const CartContext = createContext();

// Helper function to check if two items match (same productId and size)
const itemsMatch = (item1, item2) => {
    const size1 = item1.size || null;
    const size2 = item2.size || null;
    return item1.productId === item2.productId && size1 === size2;
};

const cartReducer = (state, action) => {
    switch (action.type) {
        case 'ADD_ITEM':
            const existingItem = state.items.find(item => itemsMatch(item, action.payload));
            if (existingItem) {
                return {
                    ...state,
                    items: state.items.map(item =>
                        itemsMatch(item, action.payload)
                            ? { ...item, quantity: item.quantity + action.payload.quantity }
                            : item
                    )
                };
            } else {
                return {
                    ...state,
                    items: [...state.items, action.payload]
                };
            }
        
        case 'REMOVE_ITEM':
            return {
                ...state,
                items: state.items.filter(item => !itemsMatch(item, action.payload))
            };
        
        case 'UPDATE_QUANTITY':
            return {
                ...state,
                items: state.items.map(item =>
                    itemsMatch(item, action.payload)
                        ? { ...item, quantity: action.payload.quantity }
                        : item
                ).filter(item => item.quantity > 0)
            };
        
        case 'CLEAR_CART':
            return {
                ...state,
                items: []
            };
        
        case 'LOAD_CART':
            return {
                ...state,
                items: action.payload.items || []
            };
        
        default:
            return state;
    }
};

const initialState = {
    items: []
};

// Simple user ID generation for demo purposes
const getUserId = () => {
    let userId = localStorage.getItem('userId');
    if (!userId) {
        userId = 'user_' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('userId', userId);
    }
    return userId;
};

export const CartProvider = ({ children }) => {
    const [state, dispatch] = useReducer(cartReducer, initialState);

    // Load cart from backend or localStorage on mount
    useEffect(() => {
        const loadCart = async () => {
            try {
                // Try to load from backend first
                const userId = getUserId();
                const response = await fetch(`http://localhost:8080/rest/v1/cart/${userId}`);
                
                if (response.ok) {
                    const data = await response.json();
                    if (data.success && data.items) {
                        dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                        return;
                    }
                }
            } catch (error) {
                console.log('Backend cart not available, using localStorage');
            }

            // Fallback to localStorage
            const savedCart = localStorage.getItem('cart');
            if (savedCart) {
                try {
                    const cartData = JSON.parse(savedCart);
                    // Handle both old format (direct items array) and new format (state object)
                    const items = Array.isArray(cartData) ? cartData : (cartData.items || []);
                    dispatch({ type: 'LOAD_CART', payload: { items } });
                } catch (error) {
                    console.error('Error loading cart from localStorage:', error);
                }
            }
        };

        loadCart();
    }, []);

    // Save cart to localStorage whenever it changes
    useEffect(() => {
        localStorage.setItem('cart', JSON.stringify(state));
    }, [state]);

    const addToCart = async (productId, quantity = 1, size = null) => {
        try {
            // Try to use backend API first
            const userId = getUserId();
            const params = new URLSearchParams({
                productId: productId,
                quantity: quantity.toString()
            });
            if (size) {
                params.append('size', size);
            }
            const response = await fetch(`http://localhost:8080/rest/v1/cart/${userId}/add`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    // Update local state with backend response
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return { success: true };
                }
            }

            // Fallback to local storage approach
            const productResponse = await fetch(`http://localhost:8080/rest/v1/get?prod=${productId}`);
            const productData = await productResponse.json();
            
            if (productData.error) {
                throw new Error(productData.error);
            }

            const product = productData.product;
            const cartItem = {
                productId,
                name: product.name,
                price: product.price,
                image: product.images?.search?.resolutions?.['125X161'] || product.images?.front?.resolutions?.['125X161'],
                brandName: product.brandName,
                quantity,
                size: size || null
            };

            dispatch({ type: 'ADD_ITEM', payload: cartItem });
            return { success: true };
        } catch (error) {
            console.error('Error adding to cart:', error);
            return { success: false, error: error.message };
        }
    };

    const removeFromCart = async (productId, size = null) => {
        try {
            // Try backend API first
            const userId = getUserId();
            let url = `http://localhost:8080/rest/v1/cart/${userId}/remove?productId=${productId}`;
            if (size) {
                url += `&size=${encodeURIComponent(size)}`;
            }
            const response = await fetch(url, {
                method: 'DELETE'
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return;
                }
            }
        } catch (error) {
            console.log('Backend remove failed, using local fallback');
        }

        // Fallback to local
        dispatch({ type: 'REMOVE_ITEM', payload: { productId, size } });
    };

    const updateQuantity = async (productId, size, quantity) => {
        try {
            // Try backend API first
            const userId = getUserId();
            const params = new URLSearchParams({
                productId: productId,
                quantity: quantity.toString()
            });
            if (size) {
                params.append('size', size);
            }
            const response = await fetch(`http://localhost:8080/rest/v1/cart/${userId}/update`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return;
                }
            }
        } catch (error) {
            console.log('Backend update failed, using local fallback');
        }

        // Fallback to local
        dispatch({ type: 'UPDATE_QUANTITY', payload: { productId, size, quantity } });
    };

    const clearCart = async () => {
        try {
            // Try backend API first
            const userId = getUserId();
            const response = await fetch(`http://localhost:8080/rest/v1/cart/${userId}/clear`, {
                method: 'DELETE'
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return;
                }
            }
        } catch (error) {
            console.log('Backend clear failed, using local fallback');
        }

        // Fallback to local
        dispatch({ type: 'CLEAR_CART' });
    };

    const getCartTotal = () => {
        return state.items.reduce((total, item) => {
            const price = typeof item.price === 'number' ? item.price : 0;
            return total + (price * item.quantity);
        }, 0);
    };

    const getCartItemCount = () => {
        return state.items.reduce((count, item) => count + item.quantity, 0);
    };

    const value = {
        items: state.items,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
        getCartTotal,
        getCartItemCount
    };

    return (
        <CartContext.Provider value={value}>
            {children}
        </CartContext.Provider>
    );
};

export const useCart = () => {
    const context = useContext(CartContext);
    if (!context) {
        throw new Error('useCart must be used within a CartProvider');
    }
    return context;
};
